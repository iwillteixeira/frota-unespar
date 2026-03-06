package br.unespar.frota.repository;

import br.unespar.frota.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
