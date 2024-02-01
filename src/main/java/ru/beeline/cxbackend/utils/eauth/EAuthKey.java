package ru.beeline.cxbackend.utils.eauth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class EAuthKey {

    private String alg;
    private String kty;
    private String n;
    private String e;
    private String kid;

}
