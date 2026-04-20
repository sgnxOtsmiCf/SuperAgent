package cn.sgnxotsmicf.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    //主数据源：MySQL → MyBatis-Plus 自动使用（@Primary 关键注解）
    @Bean(name = "mysqlDataSource")
    @Primary  // 标记为主数据源，MyBatis-Plus 默认优先使用
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    //Spring AI 手动创建专属 JdbcTemplate
    @Bean(name = "mySqlJdbcTemplate")
    public JdbcTemplate mySqlJdbcTemplate() {
        return new JdbcTemplate(mysqlDataSource());
    }


    //向量数据源：PostgreSQL
    @Bean(name = "pgVectorDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.vector")
    public DataSource pgVectorDataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    //Spring AI 手动创建专属 JdbcTemplate
    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(pgVectorDataSource());
    }

    //Spring AI PGVectorStore
    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        return PgVectorStore.builder(pgVectorJdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)        // 向量维度
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();
    }
}