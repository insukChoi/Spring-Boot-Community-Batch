package com.community.batch.jobs;

import com.community.batch.domain.User;
import com.community.batch.domain.enums.UserStatus;
import com.community.batch.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class InactiveUserJobConfig {

    private final static int CHUNK_SIZE = 5;

    private final EntityManagerFactory entityManagerFactory;

    private final UserRepository userRepository;

    public InactiveUserJobConfig(EntityManagerFactory entityManagerFactory, UserRepository userRepository) {
        this.entityManagerFactory = entityManagerFactory;
        this.userRepository = userRepository;
    }

    @Bean
    public Job inactiveUserJob(JobBuilderFactory jobBuilderFactory, Step inactiveJobStep) {
        return jobBuilderFactory.get("inactiveUserJob")
                .preventRestart()
                .start(inactiveJobStep)
                .build();
    }


    @Bean
    public Step inactiveJobStep(StepBuilderFactory stepBuilderFactory, JpaPagingItemReader<User> inactiveUserJpaReader){
        return stepBuilderFactory.get("inactiveUserStep")
                .<User, User> chunk(CHUNK_SIZE)
                .reader(inactiveUserJpaReader)
                .processor(inactiveUserProcessor())
                .writer(inactiveUserWriter())
                .build();
    }

    /*@Bean
    @StepScope
    public QueueItemReader<User> inactiveUserReader(){
        List<User> oldUser = userRepository.findByUpdatedDateBeforeAndStatusEquals(
                LocalDateTime.now().minusYears(1), UserStatus.ACTIVE
        );
        return new QueueItemReader<>(oldUser);
    }*/

    @Bean
    @StepScope
    public ListItemReader<User> inactiveUserReader(){
        List<User> oldUser = userRepository.findByUpdatedDateBeforeAndStatusEquals(
                LocalDateTime.now().minusYears(1), UserStatus.ACTIVE
        );
        return new ListItemReader<>(oldUser);
    }

    @Bean(destroyMethod = "")
    @StepScope
    public JpaPagingItemReader<User> inactiveUserJpaReader(@Value("#{jobParameters[nowDate]}") Date nowDate) {
        JpaPagingItemReader<User> jpaPagingItemReader = new JpaPagingItemReader<User>() {
            @Override
            public int getPage() {
                return 0;
            }
        };
        jpaPagingItemReader.setQueryString("select u from User as u where u.updatedDate < :updatedDate and u.status = :status");
        Map<String, Object> map = new HashMap<>();
        LocalDateTime now = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault());
        map.put("updatedDate", now.minusYears(1));
        map.put("status", UserStatus.ACTIVE);

        jpaPagingItemReader.setParameterValues(map);
        jpaPagingItemReader.setEntityManagerFactory(entityManagerFactory);
        jpaPagingItemReader.setPageSize(CHUNK_SIZE);
        return jpaPagingItemReader;
    }

    public ItemProcessor<User, User> inactiveUserProcessor(){
        return User::setInactive;
    }

    /*public ItemWriter<User> inactiveUserWriter(){
        return (users -> userRepository.saveAll(users));
    }*/

    private JpaItemWriter<User> inactiveUserWriter(){
        JpaItemWriter<User> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }
}
