package application.domain.ports.out;

import application.domain.models.User;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir usuarios. */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    void delete(Long userId);

    default boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}