package at.itbh.tabloid.config;

import java.util.Optional;

public interface StyleConfig {
    FontConfig font();

    Optional<String> backgroundColor();
}