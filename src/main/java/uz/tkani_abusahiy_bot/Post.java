package uz.tkani_abusahiy_bot;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "posts")
public class Post {
    @Id
    private int messageId;
    private String title;
}
