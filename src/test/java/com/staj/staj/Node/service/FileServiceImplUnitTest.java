package com.staj.staj.Node.service;


import com.staj.staj.Node.service.enums.LinkType;
import com.staj.staj.Node.service.impl.FileServiceImpl;
import com.staj.staj.commonUtils.utils.CryptoTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileServiceImplUnitTest {
    @Mock
    private CryptoTool cryptoTool;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    void generateLink_shouldReturnCorrectUrl() {
        Long docId = 42L;
        LinkType type = LinkType.GET_DOC;
        String fakeHash = "hashed42";
        String serverAddress = "localhost:8080";
        fileService.linkAddress = serverAddress;
        when(cryptoTool.hashOf(docId)).thenReturn(fakeHash);
        String link = fileService.generateLink(docId, type);
        assertThat(link)
                .isEqualTo("http://" + serverAddress + "/" + type + "?id=" + fakeHash);
    }
}
