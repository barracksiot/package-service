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

package io.barracks.packageservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.io.InputStream;
import java.util.Optional;

@CompoundIndex(name = "versionId_userId_idx", def = "{'versionId' : 1, 'userId' : 1}", unique = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageInfo {

    @Id
    private final String id;
    private final String md5;
    private final long size;
    private final String fileName;
    private final String userId;
    private final String versionId;
    @JsonIgnore
    private final InputStream inputStream;

    public PackageInfo(String id, String fileName, String md5, long size, String userId, String versionId, InputStream inputStream) {
        this.id = id;
        this.md5 = md5;
        this.size = size;
        this.fileName = fileName;
        this.userId = userId;
        this.versionId = versionId;
        this.inputStream = inputStream;
    }

    public String getId() {
        return id;
    }

    public String getMd5() {
        return md5;
    }

    public long getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUserId() {
        return userId;
    }

    public String getVersionId() {
        return versionId;
    }

    public Optional<InputStream> getInputStream() {
        return Optional.ofNullable(inputStream);
    }

    @Override
    public String toString() {
        return "PackageInfo{" +
                "id='" + id + '\'' +
                ", md5='" + md5 + '\'' +
                ", size=" + size +
                ", fileName='" + fileName + '\'' +
                ", userId='" + userId + '\'' +
                ", versionId='" + versionId + '\'' +
                ", inputStream=" + inputStream +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageInfo that = (PackageInfo) o;

        if (size != that.size) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (md5 != null ? !md5.equals(that.md5) : that.md5 != null) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (versionId != null ? !versionId.equals(that.versionId) : that.versionId != null) return false;
        return inputStream != null ? inputStream.equals(that.inputStream) : that.inputStream == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (md5 != null ? md5.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        result = 31 * result + (inputStream != null ? inputStream.hashCode() : 0);
        return result;
    }
}
