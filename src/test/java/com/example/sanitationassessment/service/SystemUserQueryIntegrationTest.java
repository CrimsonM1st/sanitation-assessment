package com.example.sanitationassessment.service;

import com.example.sanitationassessment.domain.UserRole;
import com.example.sanitationassessment.dto.user.QuerySystemUserRequest;
import com.example.sanitationassessment.dto.user.SystemUserResponse;
import com.example.sanitationassessment.entity.SystemUserEntity;
import com.example.sanitationassessment.mapper.SystemUserMapper;
import com.example.sanitationassessment.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class SystemUserQueryIntegrationTest {

    @Autowired
    private SystemUserMapper systemUserMapper;
    @Autowired
    private SystemUserService systemUserService;


    @Test
    void queryShouldFilterByUsernamePrefixRoleAndEnabled() {
        SystemUserEntity user1 = new SystemUserEntity(null, "admin-one", "hash1", UserRole.ADMIN, true,
                LocalDateTime.of(2026, 8, 7, 9, 0), LocalDateTime.of(2026, 8, 7, 9, 0));
        SystemUserEntity user2 = new SystemUserEntity(null, "admin-disabled", "hash2", UserRole.ADMIN, false,
                LocalDateTime.of(2026, 8, 7, 9, 0), LocalDateTime.of(2026, 8, 7, 9, 0));
        SystemUserEntity user3 = new SystemUserEntity(null, "inspector-one", "hash3", UserRole.INSPECTOR, true,
                LocalDateTime.of(2026, 8, 7, 9, 0), LocalDateTime.of(2026, 8, 7, 9, 0));
        systemUserMapper.insert(List.of(user1, user2, user3));

        QuerySystemUserRequest querySystemUserRequest = new QuerySystemUserRequest(
                1,
                10,
                " admin ",
                UserRole.ADMIN,
                true
        );

        PageResult<SystemUserResponse> query = systemUserService.query(querySystemUserRequest);

        assertEquals(1, query.getTotal());
        assertEquals(1, query.getRecords().size());
        assertEquals("admin-one", query.getRecords().getFirst().getUsername());
        assertEquals(UserRole.ADMIN, query.getRecords().getFirst().getRole());
        assertTrue(query.getRecords().getFirst().getEnabled());
        assertEquals(1, query.getPageNum());
        assertEquals(10, query.getPageSize());
    }

}
