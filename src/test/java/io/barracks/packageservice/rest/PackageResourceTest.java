/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.packageservice.rest;

import io.barracks.packageservice.Application;
import io.barracks.packageservice.manager.PackageManager;
import io.barracks.packageservice.manager.exception.InvalidPackageVersionException;
import io.barracks.packageservice.manager.exception.PackageConflictException;
import io.barracks.packageservice.model.PackageInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class PackageResourceTest {

    private MockMvc mvc;

    @Mock
    private PackageManager packageManager;

    @InjectMocks
    private PackageResource packageResource = new PackageResource();

    @Before
    public void setUp() throws Exception {
        mvc = MockMvcBuilders.standaloneSetup(packageResource).build();
    }

    @Test
    public void uploadPackage_whenTheRequestIsValid_shouldSendTheFileToTheManagerAndReturnAPackageInfo() throws Exception {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final PackageInfo expectedPackageInfo = new PackageInfo("OBJECTID", "Example.exe", "MD5", bytes.length, userId, versionId, null);
        final MockMultipartFile multipartFile = new MockMultipartFile("file", expectedPackageInfo.getFileName(), "application/x-msdownload", bytes);
        when(packageManager.save(eq(expectedPackageInfo.getFileName()), anyString(), isA(InputStream.class), eq(userId), eq(versionId))).thenReturn(expectedPackageInfo);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.fileUpload("/packages").file(multipartFile).param(PackageResource.USER_KEY, userId).param(PackageResource.VERSION_KEY, versionId)
        );

        // Then
        verify(packageManager).save(eq(expectedPackageInfo.getFileName()), anyString(), isA(InputStream.class), eq(userId), eq(versionId));
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id").value(expectedPackageInfo.getId()))
                .andExpect(jsonPath("$.fileName").value(expectedPackageInfo.getFileName()))
                .andExpect(jsonPath("$.md5").value(expectedPackageInfo.getMd5()))
                .andExpect(jsonPath("$.size").value((int) expectedPackageInfo.getSize()));
    }

    @Test
    public void uploadPackage_whenIOException_shouldSendBadRequest() throws Exception {
        // Given
        MockMultipartFile ioeMultipartFile = new MockMultipartFile("file", (byte[]) null) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Mocked IOE");
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Mocked IOE");
            }
        };

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .fileUpload("/packages")
                        .file(ioeMultipartFile)
                        .param(PackageResource.USER_KEY, "user")
                        .param(PackageResource.VERSION_KEY, "version")
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void uploadPackage_whenManagerThrowPackageConflictException_shouldReturnConflict() throws Exception {
        // Given
        when(packageManager.save(anyString(), anyString(), any(), anyString(), anyString())).thenThrow(new PackageConflictException("Mock Exception"));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .fileUpload("/packages")
                        .file(new MockMultipartFile("file", (byte[]) null))
                        .param(PackageResource.USER_KEY, "user")
                        .param(PackageResource.VERSION_KEY, "version")
        );

        // Then
        verify(packageManager).save(anyString(), anyString(), any(), anyString(), anyString());
        result.andExpect(status().isConflict());
    }

    @Test
    public void uploadPackage_whenManagerThrowInvalidPackageVersionException_shouldReturnBadRequest() throws Exception {
        // Given
        when(packageManager.save(anyString(), anyString(), any(), anyString(), anyString())).thenThrow(new InvalidPackageVersionException("Mock Exception"));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .fileUpload("/packages")
                        .file(new MockMultipartFile("file", (byte[]) null))
                        .param(PackageResource.USER_KEY, "user")
                        .param(PackageResource.VERSION_KEY, "version")
        );

        // Then
        verify(packageManager).save(anyString(), anyString(), any(), anyString(), anyString());
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getPackageDetails_whenPackageNotFound_shouldReturn404() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        when(packageManager.findById(packageId)).thenReturn(Optional.empty());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/" + packageId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(packageManager).findById(packageId);
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getPackageDetails_whenPackageFound_shouldReturnDetails() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        final PackageInfo info = new PackageInfo(packageId, "filename", "md5Hash", 42, "root", "v0.1", null);
        when(packageManager.findById(packageId)).thenReturn(Optional.of(info));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/" + packageId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("id").value(info.getId()))
                .andExpect(jsonPath("fileName").value(info.getFileName()))
                .andExpect(jsonPath("md5").value(info.getMd5()))
                .andExpect(jsonPath("size").value((int) info.getSize()))
                .andExpect(jsonPath("userId").value(info.getUserId()))
                .andExpect(jsonPath("versionId").value(info.getVersionId()));
        verify(packageManager).findById(packageId);
    }

    @Test
    public void getPackageContent_whenPackageNotFound_shouldReturn404() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        when(packageManager.findById(packageId)).thenReturn(Optional.empty());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/" + packageId + "/file")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
        );

        // Then
        result.andExpect(status().isNotFound());
        verify(packageManager).findById(packageId);
    }

    @Test
    public void getPackageContent_whenPackageFound_shouldReturnFileStream() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        final PackageInfo info = new PackageInfo(packageId, "filename", "md5Hash", 42, "root", "vTest", getClass().getResourceAsStream("findById_stream.txt"));
        when(packageManager.findById(packageId)).thenReturn(Optional.of(info));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/" + packageId + "/file")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
        );

        // Then
        verify(packageManager).findById(packageId);
        result.andExpect(status().isOk())
                .andExpect(content().bytes("success".getBytes(Charset.forName("UTF-8"))));
    }

    @Test
    public void getAllPackages_whenNoPackage_shouldReturnEmptyCollection() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        when(packageManager.getAllPackages(userId)).thenReturn(Collections.emptyList());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/all?userId=" + userId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(packageManager).getAllPackages(userId);
        result.andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    public void getAllPackages_whenManyPackages_shouldReturnCollection() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final ArrayList<PackageInfo> infos = new ArrayList<>();
        PackageInfo info = new PackageInfo(UUID.randomUUID().toString(), "fileA", "md5", 42, userId, UUID.randomUUID().toString(), null);
        infos.add(info);
        info = new PackageInfo(UUID.randomUUID().toString(), "fileB", "md5", 43, userId, UUID.randomUUID().toString(), null);
        infos.add(info);
        info = new PackageInfo(UUID.randomUUID().toString(), "fileC", "md5", 44, userId, UUID.randomUUID().toString(), null);
        infos.add(info);
        when(packageManager.getAllPackages(userId)).thenReturn(infos);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/packages/all?userId=" + userId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(packageManager).getAllPackages(userId);
        ResultActions resultActions = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(infos.size())));
        for (int i = 0; i < infos.size(); i++) {
            resultActions.andExpect(jsonPath("$[" + i + "].id", is(infos.get(i).getId())))
                    .andExpect(jsonPath("$[" + i + "].userId", is(infos.get(i).getUserId())))
                    .andExpect(jsonPath("$[" + i + "].versionId", is(infos.get(i).getVersionId())))
                    .andExpect(jsonPath("$[" + i + "].fileName", is(infos.get(i).getFileName())))
                    .andExpect(jsonPath("$[" + i + "].md5", is(infos.get(i).getMd5())));
        }
    }
}