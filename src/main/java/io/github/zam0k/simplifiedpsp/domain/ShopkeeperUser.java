package io.github.zam0k.simplifiedpsp.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shopkeeper_user")
@Getter @Setter @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public final class ShopkeeperUser implements IPayee {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(updatable = false, nullable = false)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    @Column(nullable = false, unique = true, length = 18, updatable = false)
    private String cnpj;
    @Column(nullable = false, unique = true)
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private BigDecimal balance;

    @Override
    public void receiveValue(BigDecimal value) {
        setBalance(getBalance().add(value));
    }
}