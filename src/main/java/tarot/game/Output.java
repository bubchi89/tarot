package tarot.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies that a parameter is modified by the function and intended to be used by the caller
 */
@Retention(RetentionPolicy.SOURCE)
@Target(value = { ElementType.PARAMETER })
public @interface Output {
}
