package uz.tkani_abusahiy_bot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface PostRepository extends JpaRepository<Post, Integer> {
    List<Post> findByTitle(String title);

    @Query(nativeQuery = true, value = "select distinct title from posts")
    List<String> findAllTitle();

    void deleteByTitle(String data);
}
