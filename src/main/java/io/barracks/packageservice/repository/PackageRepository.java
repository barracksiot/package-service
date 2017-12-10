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

package io.barracks.packageservice.repository;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import io.barracks.packageservice.model.PackageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class PackageRepository {
    static final String USER_ID_KEY = "userId";
    static final String VERSION_ID_KEY = "versionId";

    private final MongoOperations operations;
    private final GridFsTemplate gridFsTemplate;
    private final String bucket;

    @Autowired
    public PackageRepository(
            @Value("${io.barracks.packageservice.gridfs.bucket}") String bucket,
            MongoOperations operations, MongoDbFactory factory) {
        this.operations = operations;
        this.bucket = bucket;
        this.gridFsTemplate = new GridFsTemplate(factory, operations.getConverter(), bucket);
    }

    public Collection<PackageInfo> getAllPackages(String userId) {
        /* db.packages.files.aggregate(
            [
                {$match : {"metadata.userId" : ""}},
                {$sort:{"metadata.versionId":1}},
                {$project:{"_id":1, "versionId":"$metadata.versionId", "userId": "$metadata.userId"}}
            ]
        )
        * */
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("metadata.userId").is(userId)),
                Aggregation.sort(Sort.Direction.ASC, "metadata.versionId"),
                Aggregation.project("id", "md5")
                        .and("metadata.versionId").as("versionId")
                        .and("metadata.userId").as("userId")
                        .and("filename").as("fileName")
                        .and("length").as("size")
        );
        AggregationResults<PackageInfo> results = operations.aggregate(aggregation, bucket + ".files", PackageInfo.class);
        return results.getMappedResults();
    }

    public PackageInfo savePackage(PackageInfo info, String contentType) {
        final GridFSFile file = gridFsTemplate.store(
                info.getInputStream().get(),
                info.getFileName(),
                contentType,
                new BasicDBObject(USER_ID_KEY, info.getUserId()).append(VERSION_ID_KEY, info.getVersionId())
        );
        return new PackageInfo(
                file.getId().toString(),
                file.getFilename(),
                file.getMD5(),
                file.getLength(),
                (String) file.getMetaData().get(USER_ID_KEY),
                (String) file.getMetaData().get(VERSION_ID_KEY),
                null
        );
    }

    public Optional<PackageInfo> findByUserIdAndVersionId(String userId, String versionId) {
        final GridFSDBFile file = gridFsTemplate.findOne(Query.query(GridFsCriteria.whereMetaData().is(new BasicDBObject(USER_ID_KEY, userId).append(VERSION_ID_KEY, versionId))));
        return fileToPackageInfo(file);
    }

    public Optional<PackageInfo> findById(String id) {
        final GridFSDBFile file = gridFsTemplate.findOne(Query.query(GridFsCriteria.where("_id").is(id)));
        return fileToPackageInfo(file);
    }


    private Optional<PackageInfo> fileToPackageInfo(GridFSDBFile file) {
        PackageInfo info = null;
        if (file != null) {
            info = new PackageInfo(
                    file.getId().toString(),
                    file.getFilename(),
                    file.getMD5(),
                    file.getLength(),
                    file.getMetaData().get(USER_ID_KEY).toString(),
                    file.getMetaData().get(VERSION_ID_KEY).toString(),
                    file.getInputStream()
            );
        }
        return Optional.ofNullable(info);
    }

}
