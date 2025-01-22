package com.aivle.project.controller;

import com.aivle.project.dto.AccountDto;
import com.aivle.project.entity.AccountEntity;
import com.aivle.project.entity.EmployeeEntity;
import com.aivle.project.repository.AccountRepository;
import com.aivle.project.repository.EmployeeRepository;
import com.aivle.project.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;


    @GetMapping("/account")
    public String account(Model model,
                          @RequestParam(value="page", defaultValue="1") int page,
                          @RequestParam(value="keyword", required=false) String keyword) {

      //  int pageSize = 10;  // 한 페이지에 보여주는 데이터 개수

        // 페이징 정보 가져오기
        Page<AccountEntity> paging = getPagingInfo(page, keyword);

        // 페이징 관련 변수 설정
        int totalPages = paging.getTotalPages(); // 총 페이지
        int currentPage = page; // 현재 페이지 url 에서 page 변수 가져오기
        int startPage = getStartPage(currentPage);
        int endPage = getEndPage(currentPage, totalPages);
        int nextPage = getNextPage(currentPage, totalPages);
        long totalAccounts = accountService.getTotalAccountCount(); // 총 계정 수를 가져오는 메서드


        // 페이지 번호 리스트 생성
        List<Map<String, Object>> pageNumbers = getPageNumbers(startPage, endPage, currentPage);


        // 현재 로그인한 직원의 계정 수 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmployeeId = authentication.getName();
        Long currentEmployeeAccountCount = accountService.getAccountCountForEmployee(currentEmployeeId);

        // 모델에 데이터 추가
        addDataToModel(model, paging, pageNumbers, currentPage, nextPage, totalPages, keyword, totalAccounts, currentEmployeeId, currentEmployeeAccountCount);

        return "account/account_read";

    }

    // 모델에 데이터 추가
    private void addDataToModel(Model model, Page<AccountEntity> paging,
                                List<Map<String, Object>> pageNumbers,
                                int currentPage, int nextPage, int totalPages, String keyword,
                                long totalAccounts, String currentEmployeeId, Long currentEmployeeAccountCount) {

        model.addAttribute("paging", paging);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("nextPage", nextPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("currentEmployeeId", currentEmployeeId);
        model.addAttribute("currentEmployeeAccountCount", currentEmployeeAccountCount);
    }


    // 페이지 리스트 받아오기
    private List<Map<String, Object>> getPageNumbers(int startPage, int endPage, int currentPage) {
        return IntStream.rangeClosed(startPage, endPage)
                .mapToObj(pageNum -> {
                    Map<String, Object> pageInfo = new HashMap<>();
                    pageInfo.put("number", pageNum);
                    pageInfo.put("isCurrentPage", pageNum == currentPage);
                    return pageInfo;
                })
                .collect(Collectors.toList());
    }

    // 다음 페이지로 이동
    private int getNextPage(int currentPage, int totalPages) {
        return Math.min(currentPage + 1, totalPages);
    }

    // 시작페이지
    private int getStartPage(int currentPage) {
        return Math.max(1, currentPage - 5);
    }

    // 마지막 페이지
    private int getEndPage(int currentPage, int totalPages) {
        return Math.min(currentPage + 5, totalPages);
    }

    // 검색 keyword 유무 , 테이블 목록 조회
    private Page<AccountEntity> getPagingInfo(int page, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return accountService.searchAccounts(keyword, PageRequest.of(page - 1, 10));
        } else {
            return accountService.readAccount(PageRequest.of(page - 1, 10));
        }
    }

    // 계정 상세 페이지
    @GetMapping("/account/detail/{accountId}")
    public String accountDetail(@PathVariable Long accountId, Model model) {
        AccountEntity account = accountService.searchAccount(accountId);

        List<AccountEntity> accounts = accountRepository.findAll();
        List<EmployeeEntity> employee = employeeRepository.findAll();
        List<AccountEntity> activeAccounts = accountRepository.findByAccountStatus("Active");

        model.addAttribute("account", account);
        model.addAttribute("employee", employee);
        model.addAttribute("accounts", activeAccounts);

        return "account/account_detail";
    }

    // 계정 생성 페이지 (초기값으로 페이지 생성)
    @GetMapping("/account/detail/create")
    public String accountCreate(Model model) {

        AccountEntity account = new AccountEntity();

        List<AccountEntity> accounts = accountRepository.findAll();
        List<EmployeeEntity> employee = employeeRepository.findAll();
        List<AccountEntity> activeAccounts = accountRepository.findByAccountStatus("Active");

        // 초기값 설정
        account.setAccountName("");
        account.setAccountType("");
        account.setWebsite("");
        account.setContact("");
        account.setBusinessType("");
        account.setAccountManager("");
        account.setAccountDetail("");
        account.setAddress("");
        account.setAccountManagerContact("");
        account.setAccountStatus("");
        account.setAccountCreatedDate(LocalDate.now());

        account.setEmployeeId(new EmployeeEntity());
        account.setParentAccount(new AccountEntity());

        model.addAttribute("account", account);
        model.addAttribute("employee", employee);
        model.addAttribute("accounts", activeAccounts);

        return "account/account_detail";
    }

    // 새 계정 생성
    @PostMapping("/account/detail/create")
    public String accountCreateNew(@ModelAttribute AccountDto accountDto) {
            accountService.createAccount(accountDto);

            return "redirect:/account";
    }


    // 계정 정보 수정
    @PostMapping("/account/detail/{accountId}/update")
    public String accountUpdate(@PathVariable("accountId") Long accountId,  @ModelAttribute AccountDto accountDto) {
        accountService.updateAccount(accountId, accountDto);
        return "redirect:/account";
    }

    // 단일 계정 삭제
    @GetMapping("/account/detail/{accountId}/delete")
    public String accountDeleteDetail(@PathVariable("accountId") Long accountId) {
        accountService.delete(accountId);
        return "redirect:/account";
    }

    // 다중 계정 삭제
    @PostMapping("/account/detail/delete")
    public ResponseEntity<Void> deleteAccounts(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        System.out.println("deleteAccounts Received IDs: " + ids);
        accountService.deleteByIds(ids);
        return ResponseEntity.ok().build();
    }


}



