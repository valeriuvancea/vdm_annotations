package org.vdm.test;

public enum MessageField {
    TYPE("type"), CONTROLLER_ID("controller_id"), CERT_CT("cert_ct"), CERT_KV("cert_kv"),
    ENCRYPTED_CHALLENGE("encrypted_challenge"), DECRYPTED_CHALLENGE("decrypted_challenge"), PK_EFF("pk_eff"),
    HASH("hash"), CERT_EFF("cert_eff"), CERT_CA("cert_ca"), SENDER_ID("sender_id"), ENCRYPTED_DATA("encrypted_data");

    private final String text;

    MessageField(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
