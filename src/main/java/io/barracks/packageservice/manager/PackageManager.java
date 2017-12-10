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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

@Service
public class PackageManager {
    private final PackageRepository packageRepository;

    @Autowired
    public PackageManager(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public PackageInfo save(String originalFilename, String contentType, InputStream inputStream, String userId, String versionId) {
        if ("".equals(versionId.trim())) {
            throw new InvalidPackageVersionException("Version id cannot be empty");
        }
        Optional<PackageInfo> info = packageRepository.findByUserIdAndVersionId(userId, versionId);
        if (info.isPresent()) {
            throw new PackageConflictException("Version " + versionId + " already exists for user " + userId);
        }
        PackageInfo toSave = new PackageInfo(null, originalFilename, null, -1, userId, versionId, inputStream);
        return packageRepository.savePackage(toSave, contentType);
    }

    public Optional<PackageInfo> findById(String packageId) {
        return packageRepository.findById(packageId);
    }

    public Collection<PackageInfo> getAllPackages(String userId) {
        return packageRepository.getAllPackages(userId);
    }
}
