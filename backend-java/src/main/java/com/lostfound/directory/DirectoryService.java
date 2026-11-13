package com.lostfound.directory;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DirectoryService {

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record SchoolListResult(List<School> items, Pagination pagination) {
    }

    private static final int MAX_NAME = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final DirectoryStore store;

    public DirectoryService(DirectoryStore store) {
        this.store = store;
    }

    public SchoolListResult searchSchools(String keyword, int page, int pageSize) {
        validatePage(page, pageSize);
        List<School> all = store.listSchools(keyword);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<School> items = from >= total ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new SchoolListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public List<Campus> listCampuses(String schoolId) {
        requireSchool(schoolId);
        return store.listCampuses(schoolId, true);
    }

    public Campus requestCampus(String schoolId, String name) {
        requireSchool(schoolId);
        String normalized = requireName(name);
        boolean duplicate = store.listCampuses(schoolId, false).stream()
            .anyMatch(c -> c.name().equals(normalized));
        if (duplicate) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return store.createCampus(schoolId, normalized);
    }

    public Campus reviewCampus(String campusId, String decision) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String status = "APPROVE".equals(normalized) ? "ACTIVE" : "REJECTED";
        Campus campus = store.setCampusStatus(campusId, status);
        if (campus == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return campus;
    }

    // ---- 供 account / listing 复用的只读查询（返回 null 而非抛错，由调用方决定错误码）----

    public School findSchool(String schoolId) {
        return schoolId == null ? null : store.findSchool(schoolId);
    }

    public Campus findActiveCampus(String schoolId, String campusId) {
        Campus campus = store.findCampus(campusId);
        if (campus == null || !campus.schoolId().equals(schoolId) || !"ACTIVE".equals(campus.status())) {
            return null;
        }
        return campus;
    }

    public String findSchoolName(String schoolId) {
        School school = findSchool(schoolId);
        return school == null ? null : school.name();
    }

    public String findCampusName(String campusId) {
        if (campusId == null) {
            return null;
        }
        Campus campus = store.findCampus(campusId);
        return campus == null ? null : campus.name();
    }

    private School requireSchool(String schoolId) {
        School school = store.findSchool(schoolId);
        if (school == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return school;
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > MAX_NAME) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return name.trim();
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
