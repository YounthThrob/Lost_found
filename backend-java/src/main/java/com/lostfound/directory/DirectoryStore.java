package com.lostfound.directory;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存学校/校区目录存储。walking skeleton 阶段使用内存，后续以 JPA 持久化替换。 */
@Component
public class DirectoryStore {

    private final Map<String, School> schools = new ConcurrentHashMap<>();
    private final Map<String, Campus> campuses = new ConcurrentHashMap<>();

    public DirectoryStore() {
        seed();
    }

    public List<School> listSchools(String keyword) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        return schools.values().stream()
            .filter(School::enabled)
            .filter(s -> kw == null || kw.isEmpty()
                || s.name().toLowerCase().contains(kw)
                || s.aliases().stream().anyMatch(a -> a.toLowerCase().contains(kw)))
            .sorted((a, b) -> a.name().compareTo(b.name()))
            .toList();
    }

    public School findSchool(String schoolId) {
        School school = schools.get(schoolId);
        return school == null || !school.enabled() ? null : school;
    }

    public List<Campus> listCampuses(String schoolId, boolean activeOnly) {
        return campuses.values().stream()
            .filter(c -> c.schoolId().equals(schoolId))
            .filter(c -> !activeOnly || "ACTIVE".equals(c.status()))
            .sorted((a, b) -> a.name().compareTo(b.name()))
            .toList();
    }

    public Campus findCampus(String campusId) {
        return campuses.get(campusId);
    }

    public Campus createCampus(String schoolId, String name) {
        Campus campus = new Campus(UUID.randomUUID().toString(), schoolId, name, "PENDING",
            Instant.now().toString());
        campuses.put(campus.id(), campus);
        return campus;
    }

    public Campus setCampusStatus(String campusId, String status) {
        Campus campus = campuses.get(campusId);
        if (campus == null) {
            return null;
        }
        Campus updated = new Campus(campus.id(), campus.schoolId(), campus.name(), status,
            campus.createdAt());
        campuses.put(campusId, updated);
        return updated;
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        schools.clear();
        campuses.clear();
        seed();
    }

    private void seed() {
        addSchool("018c0000-0000-7000-8000-000000000001", "南方大学", List.of("南方大学", "南方大"),
            new String[][]{{"东校区", "ACTIVE"}, {"西校区", "PENDING"}});
        addSchool("018c0000-0000-7000-8000-000000000002", "北京大学", List.of("北京大学", "北大"),
            new String[][]{{"燕园校区", "ACTIVE"}});
        addSchool("018c0000-0000-7000-8000-000000000003", "清华大学", List.of("清华大学", "清华"),
            new String[][]{{"清华园校区", "ACTIVE"}});
        addSchool("018c0000-0000-7000-8000-000000000004", "浙江大学", List.of("浙江大学", "浙大"),
            new String[][]{{"紫金港校区", "ACTIVE"}});
    }

    private void addSchool(String id, String name, List<String> aliases, String[][] campusDefs) {
        schools.put(id, new School(id, name, aliases, "SEED", true, Instant.now().toString()));
        for (String[] cd : campusDefs) {
            Campus campus = new Campus(UUID.randomUUID().toString(), id, cd[0], cd[1],
                Instant.now().toString());
            campuses.put(campus.id(), campus);
        }
    }
}
