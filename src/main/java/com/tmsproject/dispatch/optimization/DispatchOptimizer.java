package com.tmsproject.dispatch.optimization;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import com.tmsproject.dispatch.optimization.dto.AssignmentCandidate;
import com.tmsproject.dispatch.optimization.dto.OptimizationResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * "1주문-1차량 매칭, 배정된 조합의 거리 합 최소화" 문제를 OR-Tools의 CP-SAT 솔버로 푼다.
 * (요구사항 정의서 v2, 2.2절 "문제 정의" 참고)
 * <p>
 * 이 클래스는 TransportOrder/Vehicle 엔티티를 전혀 모른다.
 * 오직 AssignmentCandidate(순수 데이터)만 입력받고, 순수 계산 결과만 반환한다.
 * → DB 없이 이 클래스만 단위 테스트할 수 있고, 나중에 별도 서비스로 분리하기도 쉽다.
 *
 * 전체 흐름 요약 정리
 * 가능한 모든 조합에 스위치(변수)를 하나씩 달아두고, '주문당 스위치 최대 1개, 차량당 스위치 최대 1개'라는 규칙을 걸고,
 * 켜진 스위치들의 거리 합이 최소가 되도록 CP-SAT에게 스위치를 알아서 켜고 끄게 시킨 다음,
 * 최종적으로 어떤 스위치가 켜졌는지 확인해서 결과로 돌려준다.
 */
@Component
public class DispatchOptimizer {

    // static 블록 : 이 클래스가 처음 메모리 될 때 딱 한번만 실행하는 코드
    static {
        // OR-Tools는 내부적으로 네이티브(C++) 라이브러리를 사용하므로,
        // 사용 전에 반드시 이 native 라이브러리를 로드해야 한다. 앱 실행 중 한 번만 하면 된다.
        Loader.loadNativeLibraries();
    }

    /** 거리(km, double)를 CP-SAT이 다룰 수 있는 정수 계수로 바꾸기 위한 스케일 값.
     *  CP-SAT은 목적함수 계수로 정수만 받기 때문에, "0.01km 단위"로 소수점 둘째 자리까지 보존한다. */
    private static final long DISTANCE_SCALE = 100L;

    /** 솔버가 답을 찾는 데 허용할 최대 시간(초). 정해진 시간 안에서 찾은 최선의 답을 반환한다. */
    private static final double MAX_SOLVE_SECONDS = 5.0;

    // 최적 조홥 찾기 solver
    public OptimizationResult solve(List<AssignmentCandidate> candidates,
                                    List<Long> orderIds,
                                    List<Long> vehicleIds) {
        // 문제가 뭔지 기록해두는 객체 (수학 문제를 종이에 적는 단계)
        CpModel model = new CpModel();

        // 1. 후보 조합마다 결정변수 (0 또는 1) 생성
        // model.newBoolVar(varName) : 이 조합을 쓸 것인가?를 나타내는 0/1 변수 하나를 만드는 것
        // 후보 조합(AssignmentCandidate)마다 이 변수를 하나씩 만들어서, "어떤 후보가 어떤 변수와 연결되는지"를 Map으로 기억
        Map<AssignmentCandidate, BoolVar> varByCandidate = new LinkedHashMap<>();
        for (AssignmentCandidate candidate : candidates) {
            String varName = "x_order" + candidate.orderId() + "_vehicle" + candidate.vehicleId();
            varByCandidate.put(candidate, model.newBoolVar(varName)); // 0 또는 1만 가질 수 있는 변수 (이 조합을 쓸 것인가 아닌가 예/아니오)
        }

        // 2. 제약조건 A: 각 주문은 최대 하나의 차량에만 배정된다.
        // 특정 주문 하나에 대해, 그 주문과 관련된 모든 변수들(어떤 차량이든)을 모아서, 그 중 최대 1개만 1이 될 수 있다는 규칙을 모델에 등록
        // 이걸 모든 주문에 대해 반복하기 때문에, "각 주문은 최대 하나의 차량에만 배정된다."는 규칙이 완성
        for (Long orderId : orderIds) {
            // 전체 변수 목록에서 "지금 보고있는 이 주문과 관련된 변수들만" 걸러낸다.
            // 예를들어 주문 1번에 대해 차량 A, B, C가 후보였다면, x_order1_vehicleA, x_order1_vehicleB, x_order1_vehicleC 이 3개 변수를 걸러낸다.
            List<BoolVar> varsForOrder = varByCandidate.entrySet().stream()
                    .filter(e -> e.getKey().orderId().equals(orderId))
                    .map(Map.Entry::getValue)
                    .toList();
            if (!varsForOrder.isEmpty()) {
                // "이 변수들 중 최대 하나만 1이어야 한다."는 제약을 모델에 등록 (0개가 될 수도 있음. 그 주문이 아예 배정 안 될 수도 있기 때문)
                model.addAtMostOne(varsForOrder.toArray(new Literal[0])); // 이 변수들 중 "최대" 1개만 찬다.
            }
        }

        // 3. 제약조건 B: 각 차량은 최대 하나의 주문에만 배정된다. (멀티드롭 없음, v1 원칙 그대로)
        // 제약조건 A와 마찬가지로 똑같은 로직을 차량 기준으로 반복해서 "각 차량도 최대 하나의 주문만 맡는다."라는 규칙도 추가된다.
        for (Long vehicleId : vehicleIds) {
            List<BoolVar> varsForVehicle = varByCandidate.entrySet().stream()
                    .filter(e -> e.getKey().vehicleId().equals(vehicleId))
                    .map(Map.Entry::getValue)
                    .toList();
            if (!varsForVehicle.isEmpty()) {
                model.addAtMostOne(varsForVehicle.toArray(new Literal[0]));
            }
        }

        // 4. [1단계] 목적함수: 배정 건수를 최대화 (몇 건까지 배정 가능한지 먼저 확인)
        // 중요: addAtMostOne은 "최대 1개"이지 "정확히 1개"가 아니다. 즉 "아무 변수도 선택 안 함"도
        //    이 제약조건을 만족하는 유효한 답이다. 만약 목적함수가 "거리 합 최소화" 하나뿐이라면,
        //    모든 거리값이 양수이므로 "아무것도 배정 안 함(전부 0)"이 거리 합 0으로 수학적으로 최적이 되어버려서
        //    후보가 아무리 많아도 배정 결과가 항상 0건이 되는 문제가 생긴다.
        //    그래서 "배정 건수를 최대화"를 1순위 목표로 먼저 풀고, 그 결과(최대 배정 건수)를
        //    2단계에서 제약조건으로 고정한 다음에야 거리 최소화를 2순위 목표로 푼다.
        //
        // LinearExpr : 변수들을 계수와 곱해서 더한 식
        LinearExprBuilder countObjective = LinearExpr.newBuilder();
        for (BoolVar var : varByCandidate.values()) {
            countObjective.addTerm(var, 1);
        }
        model.maximize(countObjective); // 배정건수 최대화

        CpSolver countSolver = new CpSolver();
        countSolver.getParameters().setMaxTimeInSeconds(MAX_SOLVE_SECONDS);
        CpSolverStatus countStatus = countSolver.solve(model);

        if (countStatus != CpSolverStatus.OPTIMAL && countStatus != CpSolverStatus.FEASIBLE) {
            // 후보가 아예 없거나(candidates가 비어있음) 등, 1건도 배정할 수 없는 경우
            return buildResultWithNoAssignments(orderIds, vehicleIds);
        }
        long maxAssignableCount = Math.round(countSolver.objectiveValue());

        // 5. [2단계] 1단계에서 구한 최대 배정 건수를 "정확히 이만큼은 배정해야 한다"는 제약으로 고정한 뒤,
        //    그 안에서 거리 합을 최소화하는 조합을 다시 찾는다.
        // 배정건수를 고정하고 -> minimize 거리 최적화
        model.addEquality(countObjective, maxAssignableCount); // 식의 값이 정확히 이것과 같아야 한다.

        LinearExprBuilder distanceObjective = LinearExpr.newBuilder();
        for (Map.Entry<AssignmentCandidate, BoolVar> entry : varByCandidate.entrySet()) {
            long scaledDistance = Math.round(entry.getKey().distanceKm() * DISTANCE_SCALE);
            distanceObjective.addTerm(entry.getValue(), scaledDistance);
        }
        model.minimize(distanceObjective); // 거리합 최소화 (단일 목적함수)

        // 6. 실제 풀이 실행 (최대 5초, 그 안에서 찾은 최선의 답을 사용)
        // CpModel이 문제를 적는 도구였다면, CpSolver는 그 문제를 실제로 푸는 엔진
        CpSolver solver = new CpSolver();
        // "5초 넘게 걸리면 그 시점까지 찾은 가장 좋은 답으로 만족하고 멈춰라."
        solver.getParameters().setMaxTimeInSeconds(MAX_SOLVE_SECONDS);
        // 실제로 계산을 실행하고, 결과 상태를 반환
        CpSolverStatus status = solver.solve(model);

        // 7. 결과 추출
        List<OptimizationResult.Assignment> assignments = new ArrayList<>();
        Set<Long> assignedOrderIds = new HashSet<>();
        Set<Long> assignedVehicleIds = new HashSet<>();

        // OPTIMAL: 100% 최적임이 증명된 답. FEASIBLE: 제한시간 내 찾은, 최적은 아닐 수 있지만 유효한 답.
        // INFEASIBLE : 제약조건을 만족하는 답 자체가 존재하지 않음. UNKNOWN : 제한시간 안에 아무 답도 못 찾음
        // 실무에서는 문제 규모가 커지면 OPTIMAL을 못 찾고 FEASIBLE에서 끝나는 경우가 흔하므로 둘 다 유효한 결과로 취급한다.
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            for (Map.Entry<AssignmentCandidate, BoolVar> entry : varByCandidate.entrySet()) {
                // 풀이가 끝나면, 텅 빈 변수 껍데기 (BoolVar)마다 실제로 0 또는 1이 채워진다.
                // Map으로 만들어둔 덕분에, 어떤 변수가 1인지 확인하면서, 동시에 그 변수가 어떤 AssignmentCandidate(주문, 차량, 거리)와 짝지어져있는지 바로 꺼낼 수 있다.
                // 최종 1로 확정된 변수들만 걸러서 assignments 리스트를 완성한다.
                if (solver.value(entry.getValue()) == 1) {
                    AssignmentCandidate candidate = entry.getKey();
                    assignments.add(new OptimizationResult.Assignment(candidate.orderId(), candidate.vehicleId(), candidate.distanceKm()));
                    assignedOrderIds.add(candidate.orderId());
                    assignedVehicleIds.add(candidate.vehicleId());
                }
            }
        }
        // INFEASIBLE등의 경우엔 assignments가 빈 채로 반환된다.
        // (예: 모든 조합이 용량 초과라 애초에 후보가 하나도 없는 경우)

        List<Long> unassignedOrderIds = orderIds.stream()
                .filter(id -> !assignedOrderIds.contains(id))
                .toList();
        List<Long> unassignedVehicleIds = vehicleIds.stream()
                .filter(id -> !assignedVehicleIds.contains(id))
                .toList();

        return new OptimizationResult(assignments, unassignedOrderIds, unassignedVehicleIds);
    }

    /** 1단계(배정 건수 최대화)에서부터 풀이 실패한 경우 - 전부 미배정으로 결과를 만든다. */
    private OptimizationResult buildResultWithNoAssignments(List<Long> orderIds, List<Long> vehicleIds) {
        return new OptimizationResult(new ArrayList<>(), orderIds, vehicleIds);
    }
}
