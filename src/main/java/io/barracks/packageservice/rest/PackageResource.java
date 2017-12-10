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

import io.barracks.packageservice.manager.PackageManager;
import io.barracks.packageservice.manager.exception.InvalidPackageVersionException;
import io.barracks.packageservice.manager.exception.PackageConflictException;
import io.barracks.packageservice.model.PackageInfo;
import io.barracks.packageservice.repository.PackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping(path = "/packages")
public class PackageResource {

    public static final String FILE_KEY = "file";
    public static final String USER_KEY = "userId";
    public static final String VERSION_KEY = "versionId";

    @Autowired
    private PackageManager packageManager;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> uploadPackage(@RequestParam(FILE_KEY) MultipartFile file, @RequestParam(USER_KEY) String userId, @RequestParam(VERSION_KEY) String versionId) {
        try (final InputStream inputStream = file.getInputStream()) {
            final PackageInfo packageInfo = packageManager.save(file.getOriginalFilename(), file.getContentType(), inputStream, userId, versionId);
            return new ResponseEntity<>(packageInfo, HttpStatus.CREATED);
        } catch (IOException | InvalidPackageVersionException exception) {
            return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (PackageConflictException pme) {
            return new ResponseEntity<>(pme.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}")
    public ResponseEntity<?> getPackageDetails(@PathVariable("id") String packageId) {
        final Optional<PackageInfo> packageInfo = packageManager.findById(packageId);
        if (!packageInfo.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(packageInfo.get(), HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}/file", produces = "application/octet-stream")
    public ResponseEntity<?> getPackageContent(@PathVariable("id") String packageId) {
        final Optional<PackageInfo> packageInfo = packageManager.findById(packageId);
        if (!packageInfo.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            InputStreamResource inputStreamResource = new InputStreamResource(packageInfo.get().getInputStream().get());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentLength(packageInfo.get().getSize());
            return new ResponseEntity<>(inputStreamResource, httpHeaders, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllPackages(@RequestParam("userId") String userId) {
        final Collection<PackageInfo> packageInfos = packageManager.getAllPackages(userId);
        return new ResponseEntity<>(packageInfos, HttpStatus.OK);
    }

}
