package org.ipredencao.ipredencao_manager.model.ebd;

// Usado só internamente pelo download (EbdService -> EbdController); nunca
// serializado como JSON — o controller monta um ResponseEntity<byte[]> com os
// headers certos a partir daqui.
public record EbdMaterialContent(
        Long classId,
        Long lessonId,
        String fileName,
        String contentType,
        byte[] data
) {}
