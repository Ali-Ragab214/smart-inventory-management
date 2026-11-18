package ali.ragab.saas_multi_tennant_app.exceptions;

public class TenantProvisioningException extends BusinessException {
    public TenantProvisioningException(final String message) {
        super(message);
    }
}