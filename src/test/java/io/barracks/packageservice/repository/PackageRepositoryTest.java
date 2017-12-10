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
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.barracks.packageservice.model.PackageInfo;
import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.mock.web.MockMultipartFile;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class PackageRepositoryTest {

    private static final String TEST_DATABASE = "test";
    private static final String MONGO_HOST = "localhost";
    private static final String MONGO_BUCKET = "testbucket";

    private static MongodExecutable mongodExecutable;

    private PackageRepository packageRepository;

    private DB db;
    private DBCollection packagesCollection;


    private GridFS gridFs;


    @BeforeClass
    public static void setUpClass() throws Exception {
        prepareMongo();
    }

    private static void prepareMongo() throws IOException {
        final MongodStarter starter = MongodStarter.getDefaultInstance();

        final IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(27017, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            mongodExecutable.stop();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoClient mongo = new MongoClient(MONGO_HOST);
        String collection = MONGO_BUCKET + ".files";
        db = mongo.getDB(TEST_DATABASE);
        if (db.collectionExists(collection)) {
            db.getCollection(collection).drop();
        }
        packagesCollection = db.createCollection(collection, new BasicDBObject());

        gridFs = new GridFS(db, MONGO_BUCKET);
        gridFs.remove(Query.query(GridFsCriteria.where("_id").ne(null)).getQueryObject());

        packageRepository = new PackageRepository(
                MONGO_BUCKET,
                new MongoTemplate(mongo, TEST_DATABASE),
                new SimpleMongoDbFactory(mongo, TEST_DATABASE)
        );
    }

    @After
    public void tearDown() throws Exception {
        packagesCollection.drop();
        db.dropDatabase();
    }

    @Test
    public void findVersions_whenEmpty_shouldReturnEmptyList() {
        // Given

        // When
        Collection<PackageInfo> versions = packageRepository.getAllPackages(UUID.randomUUID().toString());

        // Then
        assertThat(versions).isEmpty();
    }

    @Test
    public void findVersions_whenUserHasNot_shouldReturnEmptyList() {
        // Given
        GridFSFile file = gridFs.createFile(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        file.setMetaData(new BasicDBObject("userId", UUID.randomUUID().toString()));
        file.save();

        // When
        Collection<PackageInfo> versions = packageRepository.getAllPackages(UUID.randomUUID().toString());

        // Then
        assertThat(versions).isEmpty();
    }

    @Test
    public void findVersions_whenUserHasOne_shouldReturnOne() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();

        setupFile(userId, versionId);
        setupFile(UUID.randomUUID().toString(), versionId);

        // When
        Collection<PackageInfo> versions = packageRepository.getAllPackages(userId);

        // Then
        assertThat(versions).hasSize(1);
        PackageInfo version = versions.iterator().next();
        assertThat(version).hasFieldOrProperty("id");
        assertThat(version).hasFieldOrPropertyWithValue("userId", userId);
        assertThat(version).hasFieldOrPropertyWithValue("versionId", versionId);
    }

    @Test
    public void findVersions_whenUserHasMany_shouldReturnManySortedByVersionIdAsc() {
        // Given
        final String userId = UUID.randomUUID().toString();

        setupFile(userId, "C");
        setupFile(userId, "A");
        setupFile(userId, "B");

        // When
        Collection<PackageInfo> versions = packageRepository.getAllPackages(userId);

        // Then
        assertThat(versions).hasSize(3);
        Iterator<PackageInfo> iterator = versions.iterator();
        PackageInfo version = iterator.next();
        assertThat(version).hasFieldOrProperty("id");
        assertThat(version).hasFieldOrPropertyWithValue("userId", userId);
        assertThat(version).hasFieldOrPropertyWithValue("versionId", "A");

        version = iterator.next();
        assertThat(version).hasFieldOrProperty("id");
        assertThat(version).hasFieldOrPropertyWithValue("userId", userId);
        assertThat(version).hasFieldOrPropertyWithValue("versionId", "B");

        version = iterator.next();
        assertThat(version).hasFieldOrProperty("id");
        assertThat(version).hasFieldOrPropertyWithValue("userId", userId);
        assertThat(version).hasFieldOrPropertyWithValue("versionId", "C");
    }

    @Test
    public void savePackage_shouldStoreTheFileInGridFSAndReturnAPackageInfo() throws IOException, NoSuchAlgorithmException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
        final String expectedMd5String = new HexBinaryAdapter().marshal(md5).toLowerCase();
        final MockMultipartFile multipartFile = new MockMultipartFile("file", "Example.exe", "application/x-msdownload", bytes);
        final String userId = UUID.randomUUID().toString();
        final String versionId = "v0.1";
        final PackageInfo toSave = new PackageInfo(null, multipartFile.getOriginalFilename(), null, -1, userId, versionId, multipartFile.getInputStream());

        // When
        final PackageInfo packageInfo = packageRepository.savePackage(toSave, multipartFile.getContentType());

        // Then
        assertThat(packageInfo).isNotNull();
        assertThat(packageInfo.getSize()).isEqualTo(bytes.length);
        assertThat(packageInfo.getMd5()).isEqualTo(expectedMd5String);

        final GridFSDBFile file = gridFs.findOne(new ObjectId(packageInfo.getId()));
        assertThat(file).isNotNull();
        assertThat(file.getLength()).isEqualTo(bytes.length);
        assertThat(file.getMD5()).isEqualTo(expectedMd5String);
        assertThat(file.getFilename()).isEqualTo(multipartFile.getOriginalFilename());
        assertThat(file.getMetaData().get(PackageRepository.USER_ID_KEY)).isEqualTo(userId);
        assertThat(file.getMetaData().get(PackageRepository.VERSION_ID_KEY)).isEqualTo(versionId);
    }

    @Test
    public void findById_whenFileExists_shouldReturnValidPackageInfo() throws NoSuchAlgorithmException, IOException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
        final String expectedMd5String = new HexBinaryAdapter().marshal(md5).toLowerCase();
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        GridFSFile file = gridFs.createFile(bytes);
        file.setMetaData(new BasicDBObject(PackageRepository.USER_ID_KEY, userId).append(PackageRepository.VERSION_ID_KEY, versionId));
        file.save();

        // When
        Optional<PackageInfo> packageInfo = packageRepository.findById(file.getId().toString());

        // Then
        assertThat(packageInfo).isPresent();
        PackageInfo info = packageInfo.get();

        assertThat(info.getInputStream()).isPresent();
        final InputStream stream = info.getInputStream().get();
        final byte[] content = new byte[bytes.length];
        assertThat(stream.read(content)).isEqualTo(bytes.length);
        assertThat(bytes).isEqualTo(content);

        assertThat(info.getMd5()).isEqualTo(expectedMd5String);
        assertThat(info.getSize()).isEqualTo(bytes.length);
        assertThat(info.getUserId()).isEqualTo(userId);
        assertThat(info.getInputStream()).isNotNull();
    }

    @Test
    public void findById_whenFileDoesNotExist_shouldReturnEmpty() {
        // Given
        String fileId = UUID.randomUUID().toString();

        // When
        Optional<PackageInfo> packageInfo = packageRepository.findById(fileId);
        assertThat(packageInfo).isNotPresent();
    }

    @Test
    public void findByVersionId_whenFileExists_shouldReturnValidPackageInfo() throws NoSuchAlgorithmException, IOException {
        // Given
        final byte[] bytes = {0, 1, 2, 3, 4, 5};
        final byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
        final String expectedMd5String = new HexBinaryAdapter().marshal(md5).toLowerCase();
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        GridFSFile file = gridFs.createFile(bytes);
        file.setMetaData(new BasicDBObject(PackageRepository.USER_ID_KEY, userId).append(PackageRepository.VERSION_ID_KEY, versionId));
        file.save();

        // When
        Optional<PackageInfo> packageInfo = packageRepository.findByUserIdAndVersionId(userId, versionId);

        // Then
        assertThat(packageInfo).isPresent();
        PackageInfo info = packageInfo.get();

        assertThat(info.getInputStream()).isPresent();
        final InputStream stream = info.getInputStream().get();
        final byte[] content = new byte[bytes.length];
        assertThat(stream.read(content)).isEqualTo(bytes.length);
        assertThat(bytes).isEqualTo(content);

        assertThat(info.getMd5()).isEqualTo(expectedMd5String);
        assertThat(info.getSize()).isEqualTo(bytes.length);
        assertThat(info.getUserId()).isEqualTo(userId);
        assertThat(info.getInputStream()).isNotNull();
    }

    @Test
    public void findByVersionId_whenFileDoesNotExist_shouldReturnEmpty() {
        // When
        Optional<PackageInfo> packageInfo = packageRepository.findByUserIdAndVersionId(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Then
        assertThat(packageInfo).isNotPresent();
    }

    private void setupFile(String userId, String versionId) {
        GridFSFile file = gridFs.createFile(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        file.setMetaData(new BasicDBObject("userId", userId).append("versionId", versionId));
        file.save();
    }
}
