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

package io.barracks.packageservice.manager;

import io.barracks.packageservice.manager.exception.InvalidPackageVersionException;
import io.barracks.packageservice.manager.exception.PackageConflictException;
import io.barracks.packageservice.model.PackageInfo;
import io.barracks.packageservice.repository.PackageRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PackageManagerTest {

    @Mock
    private PackageRepository packageRepository;

    private PackageManager packageManager;

    @Before
    public void setUp() throws Exception {
        packageManager = new PackageManager(packageRepository);
    }

    @Test
    public void save_whenAllParamsAreValid_shouldCallRepositoryAndReturnPackageInfo() throws IOException, NoSuchAlgorithmException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
        final String expectedMd5String = new HexBinaryAdapter().marshal(md5).toLowerCase();
        final MockMultipartFile multipartFile = new MockMultipartFile("file", "Example.exe", "application/x-msdownload", bytes);
        final String userId = UUID.randomUUID().toString();
        final String versionId = "v0.1";
        final InputStream inputStream = multipartFile.getInputStream();
        PackageInfo expectedToSave = new PackageInfo(null, multipartFile.getOriginalFilename(), null, -1, userId, versionId, inputStream);
        PackageInfo expected = new PackageInfo(UUID.randomUUID().toString(), multipartFile.getOriginalFilename(), expectedMd5String, bytes.length, userId, versionId, null);
        doReturn(Optional.empty()).when(packageRepository).findByUserIdAndVersionId(userId, versionId);
        doReturn(expected).when(packageRepository).savePackage(expectedToSave, multipartFile.getContentType());

        // When
        final PackageInfo saved = packageManager.save(multipartFile.getOriginalFilename(), multipartFile.getContentType(), inputStream, userId, versionId);

        // Then
        verify(packageRepository).findByUserIdAndVersionId(userId, versionId);
        verify(packageRepository).savePackage(expectedToSave, multipartFile.getContentType());
        assertThat(saved).isEqualTo(expected);
    }


    @Test
    public void save_whenVersionIdAlreadyExists_shouldThrowPackageConflictException() throws IOException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final MockMultipartFile multipartFile = new MockMultipartFile("file", "Example.exe", "application/x-msdownload", bytes);
        final String userId = UUID.randomUUID().toString();
        final String versionId = "v0.1";
        doReturn(Optional.of(new PackageInfo(null, null, null, -1, null, null, null))).when(packageRepository).findByUserIdAndVersionId(userId, versionId);

        // Then When
        assertThatExceptionOfType(PackageConflictException.class)
                .isThrownBy(() -> packageManager.save(multipartFile.getOriginalFilename(), multipartFile.getContentType(), multipartFile.getInputStream(), userId, versionId));
    }

    @Test
    public void save_whenVersionIdIsEmpty_shouldThrowInvalidPackageVersionException() throws IOException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final MockMultipartFile multipartFile = new MockMultipartFile("file", "Example.exe", "application/x-msdownload", bytes);
        final String userId = UUID.randomUUID().toString();
        final String versionId = "";

        // Then When
        assertThatExceptionOfType(InvalidPackageVersionException.class)
                .isThrownBy(() -> packageManager.save(multipartFile.getOriginalFilename(), multipartFile.getContentType(), multipartFile.getInputStream(), userId, versionId));
    }

    @Test
    public void findById_shouldForwardCallToTheRepositoryAndReturnTheResult() {
        // Given
        final Optional<PackageInfo> expected = Optional.of(new PackageInfo(null, null, null, -1, null, null, null));
        final String userId = UUID.randomUUID().toString();
        doReturn(expected).when(packageRepository).findById(userId);

        // When
        Optional<PackageInfo> result = packageManager.findById(userId);

        // Then
        verify(packageRepository).findById(userId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getAllPackages_shouldForwardCallToTheRepositoryAndReturnTheResult() {
        // Given
        final Collection<PackageInfo> expected = Collections.emptyList();
        final String userId = UUID.randomUUID().toString();
        doReturn(expected).when(packageRepository).getAllPackages(userId);

        // When
        Collection<PackageInfo> result = packageManager.getAllPackages(userId);

        // Then
        verify(packageRepository).getAllPackages(userId);
        assertThat(result).isEqualTo(expected);
    }
}