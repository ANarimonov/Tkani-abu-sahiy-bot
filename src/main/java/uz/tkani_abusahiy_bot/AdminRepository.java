package uz.tkani_abusahiy_bot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    boolean existsAdminById(Long id);

}
