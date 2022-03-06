import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MP代码生成器测试类
 *
 * @author alexpetertyler
 * @since 2022/1/8
 */
public class MybatisGeneratorTest {
    @Test
    void mybatisPlusGenerator() {
        FastAutoGenerator.create(getDataSourceConfig())
                // 全局配置
                .globalConfig(getGlobalConfig())
                // 包配置
                .packageConfig(getPackageConfig())
                // 策略配置
                .strategyConfig(getStrategyConfig())
                .execute();
    }

    private DataSourceConfig.Builder getDataSourceConfig() {
        String url = "jdbc:mysql://10.60.64.66:33306/waibao_payment?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true";
        String username = "root";
        String password = "wssy001";
        return new DataSourceConfig
                .Builder(url, username, password)
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler());
    }

    private Consumer<StrategyConfig.Builder> getStrategyConfig() {
        return builder -> builder
//                添加表名
                .addInclude(
                        "log_payment"
                )

                .entityBuilder()
                .disableSerialVersionUID()
                .enableChainModel()
                .enableLombok()
                .enableRemoveIsPrefix()
                .enableTableFieldAnnotation()
                .enableActiveRecord()
                // 乐观锁
//                .versionColumnName("version")
//                .versionPropertyName("version")
                // 逻辑删除
//                .logicDeleteColumnName("enable")
//                .logicDeletePropertyName("enable")
                .naming(NamingStrategy.underline_to_camel)
                .columnNaming(NamingStrategy.underline_to_camel)
                .addTableFills(new Column("create_time", FieldFill.INSERT))
                .addTableFills(new Column("update_time", FieldFill.INSERT_UPDATE))

                .serviceBuilder()
                .formatServiceFileName("%sService")
                .formatServiceImplFileName("%sServiceImp")

                .mapperBuilder()
                .formatMapperFileName("%sMapper");
    }

    private BiConsumer<Function<String, String>, PackageConfig.Builder> getPackageConfig() {

        return (scanner, builder) -> builder.parent("org.test")
                .moduleName("test")
                .entity("entity")
                .service("service")
                .serviceImpl("service.impl")
                .mapper("mapper")
                .controller("controller")
                .other("other");
    }

    private BiConsumer<Function<String, String>, GlobalConfig.Builder> getGlobalConfig() {

        return (scanner, builder) ->
//                作者
                builder.author("alexpetertyler")
                        .disableOpenDir()
                        .dateType(DateType.ONLY_DATE)
                        .outputDir(System.getProperty("user.dir") + "/src/main/java")
                        // Swagger 2 API Doc相关注解，Knife4j适用
//                .enableSwagger()
                        .fileOverride();
    }
}
