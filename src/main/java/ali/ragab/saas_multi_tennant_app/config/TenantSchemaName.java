package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.InvalidRequestException;

import java.util.Locale;
import java.util.regex.Pattern;

//every schema name must pass through this class to ensure it is valid and properly formatted
public final class TenantSchemaName {
    private static final String PREFIX = "tenant_";
    private static final Pattern VALID_COMPANY_CODE = Pattern.compile("^[a-z0-9_]{1,50}$");

    private TenantSchemaName() {
    }

    public static String fromCompanyCode(final String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new InvalidRequestException("Company code is required");
        }

        final String normalized = companyCode.trim().toLowerCase(Locale.ROOT);
        if (!VALID_COMPANY_CODE.matcher(normalized).matches()) {
            throw new InvalidRequestException(
                    "Company code must contain only letters, digits and underscores (max 50 characters)");
        }

        return PREFIX + normalized;
    }

    public static String quote(final String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidRequestException("Schema name is required");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
