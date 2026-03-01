package com.goaltracker.e2e.factory;

import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalFrequency;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * E2E testleri için hızlı kullanıcı ve veri oluşturucu.
 * UI'a girmeden doğrudan DB'ye yazar — test hızı için kritik.
 */
@Component
public class UserRegistrationHelper {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationHelper(UserRepository userRepository,
                                  GoalRepository goalRepository,
                                  GoalEntryRepository goalEntryRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Rastgele verified kullanıcı oluşturur.
     */
    @Transactional
    public TestUser createVerifiedUser() {
        String email = TestDataFactory.randomEmail();
        String username = TestDataFactory.randomUsername();
        String password = TestDataFactory.randomPassword();
        String displayName = TestDataFactory.randomDisplayName();
        return createVerifiedUser(email, username, password, displayName);
    }

    /**
     * Belirtilen bilgilerle verified kullanıcı oluşturur.
     */
    @Transactional
    public TestUser createVerifiedUser(String email, String username, String password, String displayName) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setEmailVerified(true);
        user.setActive(true);

        User saved = userRepository.save(user);
        return new TestUser(saved.getId(), email, username, password, displayName);
    }

    /**
     * Kullanıcıyı siler.
     */
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Belirtilen kullanıcı için hedef oluşturur.
     *
     * @return oluşturulan hedefin ID'si
     */
    @Transactional
    public Long createGoalForUser(Long userId, String title, GoalType goalType,
                                  BigDecimal targetValue, String unit,
                                  LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle(title);
        goal.setGoalType(goalType);
        goal.setTargetValue(targetValue);
        goal.setUnit(unit);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        goal.setFrequency(GoalFrequency.DAILY);
        goal.setCategory(GoalCategory.PERSONAL);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setColor("#4A90D9");

        Goal saved = goalRepository.save(goal);
        return saved.getId();
    }

    /**
     * Belirtilen hedef için giriş (entry) oluşturur.
     */
    @Transactional
    public void createEntryForGoal(Long goalId, LocalDate date, BigDecimal value) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));

        GoalEntry entry = new GoalEntry();
        entry.setGoal(goal);
        entry.setEntryDate(date);
        entry.setActualValue(value);

        goalEntryRepository.save(entry);
    }

    /**
     * Test kullanıcısı bilgilerini taşıyan record.
     */
    public record TestUser(Long id, String email, String username, String password, String displayName) {
    }
}

