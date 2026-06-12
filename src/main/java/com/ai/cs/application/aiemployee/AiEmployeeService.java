package com.ai.cs.application.aiemployee;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.ai.cs.shared.dto.SortOrderItem;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiEmployeeService {

    private final AiEmployeeRepository employeeRepository;
    private final AiEmployeeReplyStrategyRepository strategyRepository;

    public List<AiEmployee> listAll() {
        return employeeRepository.findAll();
    }

    public AiEmployee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_EMPLOYEE_NOT_FOUND));
    }

    @Transactional
    public AiEmployee create(AiEmployee employee) {
        if (employee.getStatus() == null) employee.setStatus("ENABLED");
        if (employee.getStyle() == null) employee.setStyle("PROFESSIONAL");
        if (employee.getReplyLength() == null) employee.setReplyLength("MEDIUM");
        if (employee.getAggregateInterval() == null) employee.setAggregateInterval(3);
        if (employee.getDelayInterval() == null) employee.setDelayInterval(2);
        return employeeRepository.save(employee);
    }

    @Transactional
    public AiEmployee update(Long id, AiEmployee update) {
        AiEmployee existing = getById(id);
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getAvatar() != null) existing.setAvatar(update.getAvatar());
        if (update.getGreetingMsg() != null) existing.setGreetingMsg(update.getGreetingMsg());
        if (update.getStyle() != null) existing.setStyle(update.getStyle());
        if (update.getReplyLength() != null) existing.setReplyLength(update.getReplyLength());
        if (update.getContentCheck() != null) existing.setContentCheck(update.getContentCheck());
        if (update.getAggregateInterval() != null) existing.setAggregateInterval(update.getAggregateInterval());
        if (update.getDelayInterval() != null) existing.setDelayInterval(update.getDelayInterval());
        if (update.getServiceTimeStart() != null) existing.setServiceTimeStart(update.getServiceTimeStart());
        if (update.getServiceTimeEnd() != null) existing.setServiceTimeEnd(update.getServiceTimeEnd());
        if (update.getWeekdays() != null) existing.setWeekdays(update.getWeekdays());
        if (update.getCompanyIntro() != null) existing.setCompanyIntro(update.getCompanyIntro());
        if (update.getProductIntro() != null) existing.setProductIntro(update.getProductIntro());
        if (update.getServiceScope() != null) existing.setServiceScope(update.getServiceScope());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        return employeeRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        AiEmployee e = getById(id);
        e.setStatus("DISABLED");
        employeeRepository.save(e);
    }

    // Reply strategy management
    public List<AiEmployeeReplyStrategy> getStrategies(Long employeeId) {
        return strategyRepository.findByEmployeeIdAndEnabledOrderBySortOrderAsc(employeeId, true);
    }

    @Transactional
    public AiEmployeeReplyStrategy saveStrategy(Long employeeId, AiEmployeeReplyStrategy strategy) {
        getById(employeeId); // verify exists
        strategy.setEmployeeId(employeeId);
        if (strategy.getEnabled() == null) strategy.setEnabled(true);
        if (strategy.getSortOrder() == null) strategy.setSortOrder(0);
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void deleteStrategy(Long strategyId) {
        AiEmployeeReplyStrategy s = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        s.setEnabled(false);
        strategyRepository.save(s);
    }

    @Transactional
    public void batchUpdateSortOrder(Long employeeId, List<SortOrderItem> items) {
        for (var item : items) {
            AiEmployeeReplyStrategy strategy = strategyRepository.findById(item.id())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
            if (!strategy.getEmployeeId().equals(employeeId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "策略不属于该员工");
            }
            strategy.setSortOrder(item.sortOrder());
            strategyRepository.save(strategy);
        }
    }
}
