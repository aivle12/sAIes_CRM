package com.aivle.project.controller;

import com.aivle.project.dto.*;
import com.aivle.project.entity.HistoryEntity;
import com.aivle.project.entity.LeadsEntity;
import com.aivle.project.entity.OpportunitiesCommentEntity;
import com.aivle.project.entity.OpportunitiesEntity;
import com.aivle.project.entity.*;
import com.aivle.project.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
public class OpportunitiesController {
    private final OpportunitiesService opportunitiesService;
    private final ProductsService productsService;
    private final AccountService accountService;
    private final EmployeeService employeeService;
    private final LeadsService leadsService;
    private final PaginationService paginationService;
    private final CrudLogsService crudLogsService;

    // Read page
    @GetMapping("/opportunities")
    public String opportunities(@RequestParam Map<String, String> params, Model model) {
        int page = Integer.parseInt(params.getOrDefault("page", "0"));
        int size = Integer.parseInt(params.getOrDefault("size", "10"));
        String search = params.getOrDefault("search", "");
        String sortColumn = params.getOrDefault("sortColumn", "createdDate");
        String sortDirection = params.getOrDefault("sortDirection", "desc");

        Page<OpportunitiesEntity> opportunitiesPage = opportunitiesService.readOpportunities(page, size, search, sortColumn, sortDirection);
        PaginationDto<OpportunitiesEntity> paginationDto = paginationService.createPaginationData(opportunitiesPage, page, 5);

        Map<String, Long> statusCounts = opportunitiesService.getOpportunitiesStatusCounts();
        long allCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        model.addAttribute("pagination", paginationDto);

        // 데이터 추가
        model.addAttribute("ongoingCount", statusCounts.getOrDefault("Ongoing", 0L));
        model.addAttribute("pendingCount", statusCounts.getOrDefault("Pending", 0L));
        model.addAttribute("closedCount", statusCounts.getOrDefault("Closed", 0L));
        model.addAttribute("overdueCount", statusCounts.getOrDefault("Overdue", 0L));
        model.addAttribute("allCount", allCount);

        model.addAttribute("search", search);
        model.addAttribute("sortColumn", sortColumn);
        model.addAttribute("sortDirection", sortDirection);

        return "opportunities/opportunities_read";
    }

    @GetMapping("/opportunities/bar-data")
    public ResponseEntity<Map<String, List<Integer>>> getBarData() {
        return ResponseEntity.ok(opportunitiesService.getBarData());
    }

    @GetMapping("/opportunities/chart-data")
    public ResponseEntity<Map<String, List<Integer>>> getChartData() {
        return ResponseEntity.ok(opportunitiesService.getChartData());
    }

    @GetMapping("/opportunities/detail/{opportunityId}")
    public String opportunitiesDetail(@PathVariable Long opportunityId, Model model) {
        OpportunitiesEntity opportunities = opportunitiesService.searchOpportunities(opportunityId);
        List<HistoryEntity> history = opportunitiesService.getHistoryByOpportunityId(opportunityId);
        List<OpportunitiesCommentEntity> opportunitiesComments = opportunitiesService.getCommentsByOpportunityId(opportunityId);

        // 직원 목록 추가
        List<EmployeeDto.GetId> employees = employeeService.getAllEmployeeIdsAndNamesAndDepartmentIds();
        model.addAttribute("employees", employees);

        // 디버깅을 위해 로그 출력
        System.out.println("Opportunities: " + opportunities);
        opportunitiesComments.forEach(comment -> System.out.println("Comment: " + comment.getContent() + ", Date: " + comment.getCommentCreatedDate()));

        model.addAttribute("opportunities", opportunities);

        // 히스토리 수정
        // 로딩속도를 올리기 위해 findAll -> id와 name만 가져오게 변경
        model.addAttribute("history", history);
        model.addAttribute("opportunitiesComments", opportunitiesComments);
        model.addAttribute("products", productsService.getAllProductIdsAndNames());
        model.addAttribute("accounts", accountService.getAllAccountIdsAndNames());
        model.addAttribute("employee", employeeService.getAllEmployeeIdsAndNamesAndDepartmentIds());
        model.addAttribute("leads", leadsService.getAllLeadIdsAndCompanyNames());
        return "opportunities/opportunities_detail";
    }

    // History Detail page
    @GetMapping("/opportunities/detail/{opportunityId}/history/{historyId}")
    public String opportunityHistory(@PathVariable Long historyId, @PathVariable Long opportunityId, Model model) {
        HistoryEntity opportunityHistory = opportunitiesService.searchHistory(historyId);
        model.addAttribute("history", opportunityHistory);
        model.addAttribute("opportunityId", opportunityId);
        return "opportunities/opportunities_history_detail";
    }

    @PostMapping("/opportunities/detail/createcomment")
    public String createComment(@RequestParam("content") String content, @RequestParam("opportunityId") Long opportunityId, RedirectAttributes redirectAttributes) {
        String employeeId = getCurrentUserId();
        try {
            // 기회 코멘트 생성
            opportunitiesService.createComment(content, opportunityId, employeeId);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("create", "opportunities_comment", "", "True", "Success");

            return "redirect:/opportunities/detail/" + opportunityId + "#commentSection"; // 성공 시 기회 디테일 페이지로 이동
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("create", "opportunities_comment", "", "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "기회 코멘트 생성 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }

    @GetMapping("/opportunities/detail/create")
    public String opportunitiesCreate(Model model) {
        OpportunitiesEntity opportunities = new OpportunitiesEntity();

        opportunities.setOpportunityName("");
        opportunities.setRegion("");
        opportunities.setCompanySize(0);
        opportunities.setOpportunityInquiries("");
        opportunities.setCustomerEmployee("");
        opportunities.setQuantity(0);
        opportunities.setExpectedRevenue(0);
        opportunities.setCompanyRevenue(0);
        opportunities.setOpportunityNotes("");
        opportunities.setCreatedDate(LocalDate.now());
        opportunities.setTargetCloseDate(LocalDate.now());
        opportunities.setOpportunityStatus("");
        opportunities.setSuccessRate("");

        //외래키 부분
        opportunities.setLeadId(new LeadsEntity());
        opportunities.setAccountId(new AccountEntity());
        opportunities.setProductId(new ProductsEntity());
        opportunities.setEmployeeId(new EmployeeEntity());

        model.addAttribute("opportunities", opportunities);
        model.addAttribute("products", productsService.getAllProductIdsAndNames());
        model.addAttribute("accounts", accountService.getAllAccountIdsAndNames());
        model.addAttribute("employee", employeeService.getAllEmployeeIdsAndNamesAndDepartmentIds());
        model.addAttribute("leads", leadsService.getAllLeadIdsAndCompanyNames());

        return "opportunities/opportunities_detail";
    }

    @PostMapping("/opportunities/detail/create")
    public String opportunitiesCreateNew(@ModelAttribute OpportunitiesDto opportunitiesDto, RedirectAttributes redirectAttributes) {
        try {
            // 기회 생성
            opportunitiesService.createOpportunities(opportunitiesDto);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("create", "opportunities", "", "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "기회가 성공적으로 생성되었습니다.");

            return "redirect:/opportunities"; // 성공 시 기회 목록 페이지로 이동
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("create", "opportunities", "", "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "기회 생성 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }

    @GetMapping("/opportunities/detail/{opportunityId}/history/create")
    public String historyCreate(@PathVariable Long opportunityId, Model model) {
        HistoryEntity history = new HistoryEntity();

        history.setHistoryTitle("");
        history.setCustomerRepresentative("");
        history.setHistoryDate(LocalDate.now());
        history.setHistoryTime(LocalTime.now());
        history.setMeetingPlace("");
        history.setActionTaken("");
        history.setCompanySize("");
        history.setCustomerRequirements("");
        history.setOpportunity(new OpportunitiesEntity());

        model.addAttribute("history", history);
        model.addAttribute("opportunityId", opportunityId);

        return "opportunities/opportunities_history_detail";
    }

    @PostMapping("/opportunities/detail/{opportunityId}/history/create")
    public String createHistory(@PathVariable Long opportunityId, @ModelAttribute @Valid HistoryDto historyDto,
                                BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                // 유효성 검사 실패 시 에러 메시지 출력
                return "opportunities/opportunities_history_detail"; // 에러가 있으면 폼으로 다시 이동
            }
            // 기회 히스토리 생성
            opportunitiesService.createHistory(historyDto);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("create", "opportunities_history", "", "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "기회 히스토리가 성공적으로 생성되었습니다.");

            return "redirect:/opportunities/detail/" + opportunityId; // 성공 시 기회 디테일 페이지로 이동
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("create", "opportunities_history", "", "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "기회 히스토리 생성 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }

    @PostMapping("/opportunities/detail/{opportunityId}/history/{historyId}/update")
    public String historyUpdate(@PathVariable("opportunityId") Long opportunityId,
                                @PathVariable("historyId") Long historyId,
                                @ModelAttribute @Valid HistoryDto historyDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                // 유효성 검사 실패 시 에러 메시지 출력
                return "opportunities/opportunities_history_detail"; // 에러가 있으면 폼으로 다시 이동
            }
            // 기회 히스토리 수정
            opportunitiesService.updateHistory(historyId, historyDto);

            // 성공 로그 기록
            crudLogsService.logCrudOperation("update", "opportunities_history", historyId.toString(), "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "기회 히스토리가 성공적으로 수정되었습니다.");

            return "redirect:/opportunities/detail/" + opportunityId + "/history/" + historyId;
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("update", "opportunities_history", historyId.toString(), "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "기회 수정 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }


    @PostMapping("/opportunities/detail/{opportunityId}/history/{historyId}/delete")
    public ResponseEntity<Void> historyDeleteDetail(@PathVariable("opportunityId") Long opportunityId,
                                      @PathVariable("historyId") Long historyId) {
        try {
            // 계정 삭제 실행
            opportunitiesService.deleteHistory(historyId);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("delete", "opportunities_history", historyId.toString(), "True", "Success");

            return ResponseEntity.ok().build(); // HTTP 200 응답 (삭제 성공)
        } catch (Exception e) {
            // 삭제 실패 로그 기록
            crudLogsService.logCrudOperation("delete", "opportunities_history", historyId.toString(), "False", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 응답 (삭제 실패)
        }
    }


    @PostMapping("/opportunities/detail/{opportunityId}/update")
    public String opportunitiesUpdate(@PathVariable("opportunityId") Long opportunityId, @ModelAttribute OpportunitiesDto opportunitiesDto, RedirectAttributes redirectAttributes) {
        try {
            // 기회 수정
            opportunitiesService.updateOpportunities(opportunityId, opportunitiesDto);

            // 성공 로그 기록
            crudLogsService.logCrudOperation("update", "opportunities", opportunityId.toString(), "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "기회가 성공적으로 수정되었습니다.");

            return "redirect:/opportunities/detail/" + opportunityId;
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("update", "opportunities", opportunityId.toString(), "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "기회 수정 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }


    @PostMapping("/opportunities/detail/{opportunityId}/delete")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable("opportunityId") Long opportunityId) {
        try {
            // 기회 삭제 실행
            opportunitiesService.deleteOpportunities(opportunityId);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("delete", "opportunities", opportunityId.toString(), "True", "Success");

            return ResponseEntity.ok().build(); // HTTP 200 응답 (삭제 성공)
        } catch (Exception e) {
            // 삭제 실패 로그 기록
            crudLogsService.logCrudOperation("delete", "opportunities", opportunityId.toString(), "False", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 응답 (삭제 실패)
        }
    }

    @PostMapping("/opportunities/detail/delete")
    public ResponseEntity<Void> deleteOpportunities(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        try {
            // 기회 삭제 실행
            opportunitiesService.deleteOpportunitiesByIds(ids);

            // 개별 ID에 대해 성공 로그 기록
            for (Long id : ids) {
                crudLogsService.logCrudOperation("delete", "opportunities", id.toString(), "True", "Success");
            }

            return ResponseEntity.ok().build(); // HTTP 200 응답 (삭제 성공)
        } catch (Exception e) {
            // 개별 ID에 대해 실패 로그 기록
            for (Long id : ids) {
                crudLogsService.logCrudOperation("delete", "opportunities", id.toString(), "False", "Error: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 응답 (삭제 실패)
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser"))
                ? authentication.getName()
                : null;
    }
}



