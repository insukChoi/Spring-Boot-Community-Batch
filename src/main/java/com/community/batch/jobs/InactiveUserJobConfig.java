package com.community.batch.jobs;

import com.community.batch.domain.User;
import com.community.batch.domain.enums.UserStatus;
import com.community.batch.jobs.listener.InactiveJobListener;
import com.community.batch.jobs.listener.InactiveStepListener;
import com.community.batch.repository.UserRepository;
import javafx.scene.layout.FlowPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@Configuration
public class InactiveUserJobConfig {

    private final static int CHUNK_SIZE = 8;

    private final EntityManagerFactory entityManagerFactory;

    private final UserRepository userRepository;

    public InactiveUserJobConfig(EntityManagerFactory entityManagerFactory, UserRepository userRepository) {
        this.entityManagerFactory = entityManagerFactory;
        this.userRepository = userRepository;
    }

    @Bean
    public Job inactiveUserJob(JobBuilderFactory jobBuilderFactory, InactiveJobListener inactiveJobListener, Flow multiFlow) {
        return jobBuilderFactory.get("inactiveUserJob")
                .preventRestart()
                .listener(inactiveJobListener)
                .start(multiFlow)
                .end()
                .build();
    }

    @Bean
    public Flow multiFlow(Step inactiveJobStep){
        Flow[] flows = new Flow[5];
        IntStream.range(0, flows.length).forEach( i ->
                flows[i] = new FlowBuilder<Flow>("MultiFlow" + i).from(inactiveJobFlow(inactiveJobStep)).end()
        );

        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("inactiveJobFlow");
        return flowBuilder
                .split(taskExecutor())
                .add(flows)
                .end();
    }

    private Flow inactiveJobFlow(Step inactiveJobStep){
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("inactiveJobFlow");
        return flowBuilder
                .start(new InactiveJobExecutionDecider())
                .on(FlowExecutionStatus.COMPLETED.getName()).to(inactiveJobStep)
                .on(FlowExecutionStatus.FAILED.getName()).end()
                .end();
    }


    @Bean
    public Step inactiveJobStep(StepBuilderFactory stepBuilderFactory, InactiveStepListener inactiveStepListener, JpaPagingItemReader<User> inactiveUserJpaReader
    , TaskExecutor taskExecutor){
        return stepBuilderFactory.get("inactiveUserStep")
                .<User, User> chunk(CHUNK_SIZE)
                .listener(inactiveStepListener)
                .reader(inactiveUserJpaReader)
                .processor(inactiveUserProcessor())
                .writer(inactiveUserWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(2)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor(){
        return new SimpleAsyncTaskExecutor("Batch_Task");
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
    public JpaPagingItemReader<User> inactiveUserJpaReader(@Value("#{jobParameters['nowDate']}") Date nowDate) {
        JpaPagingItemReader<User> jpaPagingItemReader = new JpaPagingItemReader<User>() {
            @Override
            public int getPage() {
                return 0;
            }
        };
        jpaPagingItemReader.setQueryString("select u from User as u where u.updatedDate < :updatedDate and u.status = :status");
        Map<String, Object> map = new HashMap<>();
        System.out.println("nowDate = " + nowDate);
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
