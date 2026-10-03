package io.ngine.proxmox.client;

import io.ngine.proxmox.client.model.Upid;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UpidTests {

    @Test
    void parsesParts() {
        Upid upid = Upid.of("UPID:pve1:0001A2B3:00C4D5E6:65F0A1B2:qmclone:9000:automation@pve!ci:");

        assertThat(upid.node()).isEqualTo("pve1");
        assertThat(upid.type()).isEqualTo("qmclone");
        assertThat(upid.id()).isEqualTo("9000");
        assertThat(upid.user()).isEqualTo("automation@pve!ci");
    }

    @Test
    void allowsEmptyId() {
        assertThat(Upid.of("UPID:pve1:0001A2B3:00C4D5E6:65F0A1B2:vzdump::root@pam:").id()).isEmpty();
    }

    @Test
    void rejectsInvalidValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> Upid.of("UPID:pve1"));
        assertThatIllegalArgumentException().isThrownBy(() -> Upid.of("not-a-upid"));
    }
}
