package cheng7.mediadesk.user;

public class UserAccount {
    private Long id;
    private String account;
    private String passwordHash;
    private String displayName;

    public UserAccount() {}

    public UserAccount(Long id, String account, String passwordHash, String displayName) {
        this.id = id;
        this.account = account;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
