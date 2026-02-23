package com.openroof.openroof.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.openroof.openroof.model.user.User;

/*Auth: Enrique Rios
    Desc: Repositorio para inicios de sesion.
    ultima modif: 21/02/2026
*/

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
}
