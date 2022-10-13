package org.commcare.formplayer.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static java.util.Optional.ofNullable;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.commcare.formplayer.util.PrototypeUtils;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.utils.FileUtils;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tests for FormDefinitionService
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@EnableConfigurationProperties(value = CacheConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class FormDefinitionServiceTest {

    private final Map<List<String>, SerializableFormDefinition> formDefinitionMap = new HashMap<>();
    @Autowired
    FormDefinitionService formDefinitionService;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    private FormDefinitionRepo formDefinitionRepo;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    private String appId;
    private String formXmlns;
    private String formVersion;
    private FormDef formDef;

    @BeforeAll
    public static void setUpAll() {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
    }

    private void mockFormDefinitionRepo() {
        // the repo always returns the saved object so simulate that in the mock
        when(this.formDefinitionRepo.save(any())).thenAnswer(new Answer<SerializableFormDefinition>() {
            @Override
            public SerializableFormDefinition answer(InvocationOnMock invocation) throws Throwable {
                SerializableFormDefinition formDef = (SerializableFormDefinition)invocation.getArguments()[0];
                formDefinitionMap.put(
                        Arrays.asList(formDef.getAppId(), formDef.getFormXmlns(), formDef.getFormVersion()),
                        formDef
                );
                return formDef;
            }
        });

        // need to mock this to test get is successful
        when(this.formDefinitionRepo.findByAppIdAndFormXmlnsAndFormVersion(any(), any(), any())).thenAnswer(
                new Answer<Optional<SerializableFormDefinition>>() {
                    @Override
                    public Optional<SerializableFormDefinition> answer(InvocationOnMock invocation)
                            throws Throwable {
                        String appId = (String)invocation.getArguments()[0];
                        String formXmlns = (String)invocation.getArguments()[1];
                        String formVersion = (String)invocation.getArguments()[2];
                        List<String> keyList = Arrays.asList(appId, formXmlns, formVersion);
                        if (formDefinitionMap.containsKey(keyList)) {
                            return Optional.of(formDefinitionMap.get(keyList));
                        }
                        return Optional.empty();
                    }
                });
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.mockFormDefinitionRepo();

        this.appId = "123456789";
        this.formXmlns = "http://openrosa.org/formdesigner/962C095E-3AB0-4D92-B9BA-08478FF94475";
        this.formVersion = "1";
        this.formDef = loadFormDef();

        PrototypeUtils.setupThreadLocalPrototypes();
    }

    @AfterEach
    public void cleanup() {
        this.cacheManager.getCache("form_definition").clear();
        getFormDefStorage().removeAll();
    }

    @Test
    public void testFormDefinitionCacheExists() {
        Cache cache = cacheManager.getCache("form_definition");
        assertNotNull(cache);
        assertTrue(cache instanceof CaffeineCache);
    }

    @Test
    public void testGetOrCreateFormDefinitionCreatesSuccessfully() {
        SerializableFormDefinition createdFormDefinition = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );
        assertNotNull(createdFormDefinition);
    }

    @Test
    public void testGetOrCreateFormDefinitionGetsSuccessfully() {
        SerializableFormDefinition createdFormDef = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );

        this.cacheManager.getCache("form_definition").clear();

        SerializableFormDefinition fetchedFormDef = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );
        assertEquals(createdFormDef, fetchedFormDef);
    }

    @Test
    public void testGetOrCreateFormDefinitionCachesSuccessfully() {
        assertEquals(Optional.empty(), getCachedFormDefinitionModel(this.appId, this.formXmlns, this.formVersion));

        SerializableFormDefinition formDefinition = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );

        assertEquals(Optional.of(formDefinition),
                getCachedFormDefinitionModel(this.appId, this.formXmlns, this.formVersion));
    }

    @Test
    public void testGetOrCreateFormDefinitionNewVersionNotEqual() {
        SerializableFormDefinition formDefinitionV1 = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );

        String updatedFormVersion = "2";
        SerializableFormDefinition formDefinitionV2 = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, updatedFormVersion, this.formDef
        );

        assertNotEquals(formDefinitionV1, formDefinitionV2);
    }

    @Test
    public void testWriteToLocalStorage() {
        this.formDefinitionService.writeToLocalStorage(this.formDef);
        FormDef formDef = getFormDefStorage().getRecordForValue("XMLNS", this.formXmlns);
        assertThat(formDef).isNotNull();
    }

    @Test
    public void testWriteToLocalStorage_FormDefExists() {
        getFormDefStorage().write(formDef);
        boolean valueWritten = this.formDefinitionService.writeToLocalStorage(this.formDef);
        assertThat(valueWritten).isFalse();
    }

    @Test
    public void testGetFormDefCaching() {
        SerializableFormDefinition formDef = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );
        String sessionId = UUID.randomUUID().toString();
        SerializableFormSession session = new SerializableFormSession(sessionId);
        session.setFormDefinition(formDef);

        assertThat(getCachedFormDefinition(sessionId)).isEmpty();
        this.formDefinitionService.getFormDef(session);
        assertThat(getCachedFormDefinition(sessionId)).isNotEmpty();
    }

    @Test
    public void testGetFormDefBrokenSerialization() {
        SerializableFormSession session = createSessionWithBrokenFormDef();

        // make sure we're not going to hit the cache
        assertThat(getCachedFormDefinition(session.getId())).isEmpty();
        FormDef reSerializedFormDef = this.formDefinitionService.getFormDef(session);

        assertThat(reSerializedFormDef.getID()).isEqualTo(this.formDef.getID());

    }

    @Test
    public void testGetFormDefFromStorageUpdatesSerializableFormDefinition() throws Exception {
        SerializableFormSession session = createSessionWithBrokenFormDef();

        FormDef reSerializedFormDef = this.formDefinitionService.getFormDef(session);

        SerializableFormDefinition updatedFormDef = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );
        assertEquals(updatedFormDef.getSerializedFormDef(),
                FormDefStringSerializer.serialize(reSerializedFormDef));

    }

    @Test
    public void testGetFormDefFromStorageUpdatesCache() throws Exception {
        SerializableFormSession session = createSessionWithBrokenFormDef();

        FormDef reSerializedFormDef = this.formDefinitionService.getFormDef(session);

        Optional<SerializableFormDefinition> cachedFormDefinition = getCachedSerializableFormDefinition(
                appId, formXmlns, formVersion
        );
        assertTrue(cachedFormDefinition.isPresent());
        assertEquals(cachedFormDefinition.get().getSerializedFormDef(),
                FormDefStringSerializer.serialize(reSerializedFormDef));

    }

    public SerializableFormSession createSessionWithBrokenFormDef() {
        SerializableFormDefinition formDef = this.formDefinitionService.getOrCreateFormDefinition(
                this.appId, this.formXmlns, this.formVersion, this.formDef
        );
        ReflectionTestUtils.setField(formDef, "serializedFormDef", "not a form def");

        formDefinitionRepo.save(formDef);

        SerializableFormSession session = new SerializableFormSession(UUID.randomUUID().toString());
        session.setFormDefinition(formDef);
        getFormDefStorage().write(this.formDef);

        return session;
    }

    private Optional<SerializableFormDefinition> getCachedFormDefinitionModel(
            String appId, String formXmlns, String formVersion) {
        List<String> idArray = Arrays.asList(appId, formXmlns, formVersion);
        return ofNullable(this.cacheManager.getCache("form_definition")).map(
                cache -> cache.get(idArray, SerializableFormDefinition.class)
        );
    }

    private Optional<FormDef> getCachedFormDefinition(String sessionId) {
        return ofNullable(this.cacheManager.getCache("form_definition")).map(
                cache -> cache.get(sessionId, FormDef.class)
        );
    }

    private Optional<SerializableFormDefinition> getCachedSerializableFormDefinition(
            String appId,
            String formXmlns,
            String formVersion
    ) {
        List<String> cacheKey = Arrays.asList(appId, formXmlns, formVersion);
        return ofNullable(this.cacheManager.getCache("form_definition")).map(
                cache -> cache.get(cacheKey, SerializableFormDefinition.class)
        );
    }

    private FormDef loadFormDef() throws Exception {
        String formXml = FileUtils.getFile(this.getClass(), "xforms/hidden_value_form.xml");
        return XFormUtils.getFormRaw(
                new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
    }

    private IStorageUtilityIndexed<FormDef> getFormDefStorage() {
        return this.storageFactory.getStorageManager().getStorage(FormDef.STORAGE_KEY);
    }

    /**
     * Only include the service under test and its dependencies
     * This should not be necessary, but we're using older versions of junit and spring
     */
    @ComponentScan(
            basePackageClasses = {FormDefinitionService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    FormDefinitionService.class})
    )
    @EnableCaching
    @Configuration
    public static class FormDefinitionServiceTestConfig {

        @MockBean
        public FormDefinitionRepo formDefinitionRepo;

        @MockBean
        public FormSessionService FormSessionService;

        @MockBean
        public JdbcTemplate jdbcTemplate;

        @Bean
        public FormplayerStorageFactory storageFactory() {
            FormplayerStorageFactory storageFactory = new FormplayerStorageFactory();
            storageFactory.configure("test", "test", "app_id", "");
            storageFactory.registerFormDefStorage();
            return storageFactory;
        };
    }
}
