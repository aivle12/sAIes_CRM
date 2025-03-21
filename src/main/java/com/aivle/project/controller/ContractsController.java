package com.aivle.project.controller;

import com.aivle.project.dto.*;
import com.aivle.project.entity.*;
import com.aivle.project.repository.*;
import com.aivle.project.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
public class ContractsController {
    private static final int DISPLAY_RANGE = 5;

    private final ContractsService contractsService;
    private final ProductsService productsService;
    private final AccountService accountService;
    private final EmployeeService employeeService;
    private final OpportunitiesService opportunitiesService;
    private final ContractsRepository contractsRepository;
    private final PaginationService paginationService;
    private final CrudLogsService crudLogsService;

    // Read page
    @GetMapping("/contracts")
    public String contracts(@RequestParam Map<String, String> params, Model model) {
        int page = Integer.parseInt(params.getOrDefault("page", "0"));
        int size = Integer.parseInt(params.getOrDefault("size", "10"));
        String search = params.getOrDefault("search", "");
        String sortColumn = params.getOrDefault("sortColumn", "startDate");
        String sortDirection = params.getOrDefault("sortDirection", "desc");

        // 데이터 조회
        Page<ContractsEntity> contractsPage = contractsService.readContracts(page, size, search, sortColumn, sortDirection);

        // 페이지네이션 데이터 생성
        PaginationDto<ContractsEntity> paginationDto = paginationService.createPaginationData(contractsPage, page, 5);


        // 상태별 계약 수 가져오기
        Map<String, Long> statusCounts = contractsService.getContractStatusCounts();
        long allCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // Model에 데이터 추가
        model.addAttribute("pagination", paginationDto);
        // 데이터 추가
        model.addAttribute("draftCount", statusCounts.getOrDefault("Draft", 0L));
        model.addAttribute("inApprovalProcessCount", statusCounts.getOrDefault("In Approval Process", 0L));
        model.addAttribute("activatedCount", statusCounts.getOrDefault("Activated", 0L));
        model.addAttribute("allCount", allCount);

        model.addAttribute("search", search);
        model.addAttribute("sortColumn", sortColumn);
        model.addAttribute("sortDirection", sortDirection);

        return "contracts/contracts_read";
    }

    @GetMapping("/contracts/bar-data")
    public ResponseEntity<Map<String, List<Integer>>> getBarData() {
        return ResponseEntity.ok(contractsService.getBarData());
    }

    @GetMapping("/contracts/chart-data")
    public ResponseEntity<Map<String, List<Integer>>> getChartData() {
        return ResponseEntity.ok(contractsService.getChartData());
    }


    @GetMapping("/contracts/detail/{contractId}")
    public String contractsDetail(@PathVariable Long contractId, Model model) {
        ContractsEntity contracts = contractsService.searchContracts(contractId);
        List<OrdersEntity> orders = contractsService.getOrdersByContractId(contractId);

        List<ProductsDto> products = productsService.getAllProductIdsAndNames();
        List<AccountDto> accounts = accountService.getAllAccountIdsAndNames();
        List<EmployeeDto.GetId> employee = employeeService.getAllEmployeeIdsAndNamesAndDepartmentIds();
        List<OpportunitiesDto> opportunities = opportunitiesService.getAllOpportunityIdsAndNames();

        model.addAttribute("contracts", contracts);
        model.addAttribute("products", products);
        model.addAttribute("accounts", accounts);
        model.addAttribute("employee", employee);
        model.addAttribute("opportunities", opportunities);
        model.addAttribute("orders", orders);

        model.addAttribute("uploadedFileName", contracts.getFileName());
        model.addAttribute("contractId", contracts.getContractId());

        return "contracts/contracts_detail";
    }

    @GetMapping("/contracts/validate")
    @ResponseBody
    public ResponseEntity<ValidationResponseDto> validateContract(@RequestParam Long contractId) {
        boolean exists = contractsRepository.existsById(contractId);

        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ValidationResponseDto("Invalid contract ID"));
        }

        return ResponseEntity.ok(new ValidationResponseDto("Valid contract ID"));
    }

    @GetMapping("/contracts/detail/create")
    public String contractsCreate(Model model) {
        ContractsEntity contracts = new ContractsEntity();

        List<ProductsDto> products = productsService.getAllProductIdsAndNames();
        List<AccountDto> accounts = accountService.getAllAccountIdsAndNames();
        List<EmployeeDto.GetId> employee = employeeService.getAllEmployeeIdsAndNamesAndDepartmentIds();
        List<OpportunitiesDto> opportunities = opportunitiesService.getAllOpportunityIdsAndNames();

        contracts.setContractStatus("");
        contracts.setStartDate(LocalDate.now());
        contracts.setTerminationDate(LocalDate.now());
        contracts.setContractDetail("");
        contracts.setContractSales(0);
        contracts.setContractAmount(0);
        contracts.setContractClassification("");

        contracts.setOpportunityId(new OpportunitiesEntity());
        contracts.setAccountId(new AccountEntity());
        contracts.setProductId(new ProductsEntity());
        contracts.setEmployeeId(new EmployeeEntity());

        model.addAttribute("contracts", contracts);
        model.addAttribute("products", products);
        model.addAttribute("accounts", accounts);
        model.addAttribute("employee", employee);
        model.addAttribute("opportunities", opportunities);

        return "contracts/contracts_detail";
    }

    @PostMapping("/contracts/detail/create")
    public String contractsCreateNew(@ModelAttribute @Valid ContractsDto contractsDto, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                // 유효성 검사 실패 시 에러 메시지 출력
                return "redirect:/contracts/detail/create"; // 에러가 있으면 폼으로 다시 이동
            }
            // 계약 생성
            contractsService.createContracts(contractsDto);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("create", "contracts", "", "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "계약이 성공적으로 생성되었습니다.");

            return "redirect:/contracts"; // 성공 시 계약 목록 페이지로 이동
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("create", "contracts", "", "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "계약 생성 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }

    @PostMapping("/contracts/detail/{contractId}/update")
    public String contractsUpdate(@PathVariable("contractId") Long contractId, @ModelAttribute @Valid ContractsDto contractsDto, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                // 유효성 검사 실패 시 에러 메시지 출력
                return "redirect:/contracts/detail/" + contractId; // 에러가 있으면 폼으로 다시 이동
            }
            // 계약 수정
            contractsService.updateContracts(contractId, contractsDto);

            // 성공 로그 기록
            crudLogsService.logCrudOperation("update", "contracts", contractId.toString(), "True", "Success");

            // 성공 메시지를 RedirectAttributes에 저장 (리다이렉트 후에도 유지됨)
            redirectAttributes.addFlashAttribute("message", "계약이 성공적으로 수정되었습니다.");

            return "redirect:/contracts/detail/" + contractId;
        } catch (Exception e) {
            // 실패 로그 기록
            crudLogsService.logCrudOperation("update", "contracts", contractId.toString(), "False", "Error: " + e.getMessage());

            // 에러 메시지를 사용자에게 전달
            redirectAttributes.addFlashAttribute("errorMessage", "계약 수정 중 오류가 발생했습니다. 다시 시도해주세요.");

            return "redirect:/errorPage"; // 에러 발생 시 오류 페이지로 리다이렉트
        }
    }

    @PostMapping("/contracts/detail/{contractId}/delete")
    public ResponseEntity<Void> deleteContract(@PathVariable("contractId") Long contractId) {
        try {
            // 계약 삭제 실행
            contractsService.deleteContracts(contractId);

            // CRUD 작업 로깅
            crudLogsService.logCrudOperation("delete", "contracts", contractId.toString(), "True", "Success");

            return ResponseEntity.ok().build(); // HTTP 200 응답 (삭제 성공)
        } catch (Exception e) {
            // 삭제 실패 로그 기록
            crudLogsService.logCrudOperation("delete", "contracts", contractId.toString(), "False", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 응답 (삭제 실패)
        }
    }

    @PostMapping("/contracts/detail/delete")
    public ResponseEntity<Void> deleteContracts(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        try {
            // 계약 삭제 실행
            contractsService.deleteContractsByIds(ids);

            // 개별 ID에 대해 성공 로그 기록
            for (Long id : ids) {
                crudLogsService.logCrudOperation("delete", "contracts", id.toString(), "True", "Success");
            }

            return ResponseEntity.ok().build(); // HTTP 200 응답 (삭제 성공)
        } catch (Exception e) {
            // 개별 ID에 대해 실패 로그 기록
            for (Long id : ids) {
                crudLogsService.logCrudOperation("delete", "contracts", id.toString(), "False", "Error: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 응답 (삭제 실패)
        }
    }

    // 파일 업로드
    @PostMapping("/contracts/detail/{contractId}/upload")
    public ResponseEntity<String> uploadFile(
            @PathVariable Long contractId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB 제한
                return ResponseEntity.badRequest().body("파일 크기는 최대 5MB를 초과할 수 없습니다.");
            }

            contractsService.saveFileToContract(contractId, file);
            return ResponseEntity.ok("파일 업로드 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/contracts/detail/{contractId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long contractId) {

        ContractsEntity contract = contractsRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));

        if (contract.getFileData() == null) {
            throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + contract.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contract.getMimeType())
                .body(contract.getFileData());
    }

    @DeleteMapping("/contracts/detail/{contractId}/file")
    public ResponseEntity<String> deleteFile(@PathVariable Long contractId) {


        ContractsEntity contract = contractsRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));

        if (contract.getFileData() == null) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("파일이 존재하지 않습니다.");
        }

        contract.setFileData(null);
        contract.setFileName(null);
        contract.setMimeType(null);

        contractsRepository.save(contract);

        return ResponseEntity.ok("파일이 성공적으로 삭제되었습니다.");
    }
}
