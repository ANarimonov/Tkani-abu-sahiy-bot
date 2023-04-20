package uz.tkani_abusahiy_bot;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity(name = "admins")
public class Admin {

    @Id
    private Long id;
    private int step = 0;

    public Admin setStep(int step) {
        this.step = step;
        return this;
    }

    public Admin(Long id) {
        this.id = id;
    }
}
