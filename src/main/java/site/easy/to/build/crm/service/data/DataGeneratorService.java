package site.easy.to.build.crm.service.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.javafaker.Faker;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@SuppressWarnings("rawtypes")
@Service
public class DataGeneratorService {
    
    @Autowired
    private ApplicationContext context;
    
    // Cache for already generated entities by type and ID
    private Map<Class<?>, List<Object>> generatedEntitiesCache = new HashMap<>();
    
    @Transactional
    public void generateDataForTable(String tableName, int recordCount) {
        // Clear cache for fresh generation
        generatedEntitiesCache.clear();
        
        // Find the repository for this table
        String entityName = getEntityNameFromTableName(tableName);
        Object repository = findRepositoryForEntity(entityName);
        
        if (repository == null) {
            throw new IllegalArgumentException("No repository found for table: " + tableName);
        }
        
        // Generate data with dependencies considered
        Class<?> entityClass;
        try {
            entityClass = Class.forName("site.easy.to.build.crm.entity." + entityName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Entity class not found for: " + entityName, e);
        }
        
        generateDataForEntity(entityClass, recordCount);
    }
    
    @Transactional
    public void generateDataForAllTables(int recordCount) {
        // Clear cache for fresh generation
        generatedEntitiesCache.clear();
        
        // Find all repositories
        Map<String, JpaRepository> repositories = context.getBeansOfType(JpaRepository.class);
        
        // Sort entities by dependency order
        List<Class<?>> sortedEntities = sortEntitiesByDependency(repositories);
        
        // Generate data in dependency order
        for (Class<?> entityClass : sortedEntities) {
            generateDataForEntity(entityClass, recordCount);
        }
    }
    
    private List<Class<?>> sortEntitiesByDependency(Map<String, JpaRepository> repositories) {
        // Create a dependency graph
        Map<Class<?>, Set<Class<?>>> dependencyGraph = new HashMap<>();
        List<Class<?>> entities = new ArrayList<>();
        
        // Build the graph
        for (JpaRepository repository : repositories.values()) {
            Class<?> entityClass = getEntityClassForRepository(repository);
            if (entityClass != null) {
                entities.add(entityClass);
                dependencyGraph.put(entityClass, getDependencies(entityClass));
            }
        }
        
        // Perform topological sort
        List<Class<?>> sorted = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> temp = new HashSet<>();
        
        for (Class<?> entity : entities) {
            if (!visited.contains(entity)) {
                topologicalSort(entity, visited, temp, dependencyGraph, sorted);
            }
        }
        
        return sorted;
    }
    
    private void topologicalSort(Class<?> entity, Set<Class<?>> visited, Set<Class<?>> temp, 
                                Map<Class<?>, Set<Class<?>>> graph, List<Class<?>> sorted) {
        temp.add(entity);
        Set<Class<?>> dependencies = graph.get(entity);
        
        if (dependencies != null) {
            for (Class<?> dependency : dependencies) {
                if (!visited.contains(dependency)) {
                    if (temp.contains(dependency)) {
                        // Circular dependency detected, but continue
                        System.err.println("Warning: Circular dependency detected for " + entity.getSimpleName());
                        continue;
                    }
                    topologicalSort(dependency, visited, temp, graph, sorted);
                }
            }
        }
        
        temp.remove(entity);
        visited.add(entity);
        sorted.add(0, entity); // Add to beginning of the list
    }
    
    private Set<Class<?>> getDependencies(Class<?> entityClass) {
        Set<Class<?>> dependencies = new HashSet<>();
        
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                dependencies.add(field.getType());
            }
        }
        
        return dependencies;
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    private void generateDataForEntity(Class<?> entityClass, int count) {
        String entityName = entityClass.getSimpleName();
        JpaRepository repository = findRepositoryForEntityClass(entityClass);
        
        if (repository == null) {
            System.err.println("Warning: No repository found for entity " + entityName);
            return;
        }
        
        System.out.println("Generating " + count + " records for " + entityName);
        
        List<Object> entities = generateEntitiesForClass(entityClass, count);
        repository.saveAll(entities);
        
        // Cache generated entities for future reference
        generatedEntitiesCache.put(entityClass, entities);
    }
    
    private List<Object> generateEntitiesForClass(Class<?> entityClass, int count) {
        Faker faker = new Faker();
        List<Object> entities = new ArrayList<>();
        
        try {
            for (int i = 0; i < count; i++) {
                Object entity = entityClass.getDeclaredConstructor().newInstance();
                
                // Process each field
                for (Field field : entityClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    
                    // Skip ID fields as they're auto-generated
                    if (field.getName().equals("id") || 
                        field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                        continue;
                    }
                    
                    // Handle relationship fields
                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        setRelationalField(field, entity);
                    } 
                    // Skip collections to avoid circular dependencies
                    else if (field.isAnnotationPresent(OneToMany.class) || 
                             Collection.class.isAssignableFrom(field.getType())) {
                        continue;
                    } 
                    // Handle regular fields
                    else {
                        setRandomValueForField(faker, entity, field);
                    }
                }
                
                entities.add(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating entities for " + entityClass.getSimpleName(), e);
        }
        
        return entities;
    }
    
    @SuppressWarnings("unchecked")
    private void setRelationalField(Field field, Object entity) throws IllegalAccessException {
        Class<?> relationClass = field.getType();
        
        // Check if we have already generated entities of this type
        List<Object> relatedEntities = generatedEntitiesCache.get(relationClass);
        
        if (relatedEntities != null && !relatedEntities.isEmpty()) {
            // Randomly select one of the already generated entities
            int randomIndex = new Random().nextInt(relatedEntities.size());
            Object relatedEntity = relatedEntities.get(randomIndex);
            field.set(entity, relatedEntity);
        } else {
            // Generate a single related entity if none exist
            JpaRepository repository = findRepositoryForEntityClass(relationClass);
            if (repository != null) {
                List<?> existingEntities = repository.findAll();
                
                if (!existingEntities.isEmpty()) {
                    // Use an existing entity from database
                    int randomIndex = new Random().nextInt(existingEntities.size());
                    field.set(entity, existingEntities.get(randomIndex));
                } else {
                    // Create a new related entity
                    Object relatedEntity = generateSingleEntityForClass(relationClass);
                    if (relatedEntity != null) {
                        repository.save(relatedEntity);
                        field.set(entity, relatedEntity);
                        
                        // Cache this entity for future reference
                        List<Object> newCache = new ArrayList<>();
                        newCache.add(relatedEntity);
                        generatedEntitiesCache.put(relationClass, newCache);
                    }
                }
            }
        }
    }
    
    private Object generateSingleEntityForClass(Class<?> entityClass) {
        Faker faker = new Faker();
        
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            
            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                
                // Skip ID, OneToMany and ManyToOne to avoid circular dependencies
                if (field.getName().equals("id") || 
                    field.isAnnotationPresent(jakarta.persistence.Id.class) ||
                    field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    Collection.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                
                setRandomValueForField(faker, entity, field);
            }
            
            return entity;
        } catch (Exception e) {
            System.err.println("Error generating related entity for " + entityClass.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private void setRandomValueForField(Faker faker, Object entity, Field field) {
        try {
            Class<?> type = field.getType();
            
            if (type == String.class) {
                // Get column length from annotations if available
                int maxLength = getMaxFieldLength(field);
                
                String value;
                // Set appropriate values based on field name
                if (field.getName().contains("email")) {
                    value = faker.internet().emailAddress();
                } else if (field.getName().equals("phone")) {
                    value = "03245664"; 
                } else if (field.getName().contains("phone")) {
                    value = faker.numerify("###-###-####"); 
                } 
                else if (field.getName().equals("firstName") || field.getName().equals("first_name")) {
                    value = faker.name().firstName();
                } else if (field.getName().equals("lastName") || field.getName().equals("last_name")) {
                    value = faker.name().lastName();
                } else if (field.getName().contains("name")) {
                    value = faker.name().fullName();
                } else if (field.getName().contains("phone")) {
                    value = faker.phoneNumber().phoneNumber();
                } else if (field.getName().contains("address")) {
                    value = faker.address().streetAddress();
                } else if (field.getName().equals("password")) {
                    value = "password123"; 
                } else if (field.getName().equals("username")) {
                    value = faker.name().username();
                } else if (field.getName().equals("provider")) {
                    value = "local";
                } else {
                    value = "hello";
                }
                
                // Ensure the value doesn't exceed the maximum length
                if (value.length() > maxLength) {
                    value = value.substring(0, maxLength);
                }
                
                field.set(entity, value);
            } else if (type == Integer.class || type == int.class) {
                field.set(entity, faker.number().numberBetween(1, 100));
            } else if (type == Long.class || type == long.class) {
                field.set(entity, faker.number().numberBetween(1L, 100L));
            } else if (type == Double.class || type == double.class) {
                field.set(entity, faker.number().randomDouble(2, 1, 100));
            } else if (type == Float.class || type == float.class) {
                field.set(entity, (float) faker.number().randomDouble(2, 1, 100));
            } else if (type == BigDecimal.class) {
                field.set(entity, BigDecimal.valueOf(faker.number().randomDouble(2, 1, 100)));
            } else if (type == LocalDate.class) {
                field.set(entity, LocalDate.now().minusDays(faker.number().numberBetween(1, 30)));
            } else if (type == LocalDateTime.class) {
                field.set(entity, LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30)));
            } else if (type == Date.class) {
                field.set(entity, Date.from(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30))
                        .atZone(ZoneId.systemDefault()).toInstant()));
            } else if (type == Boolean.class || type == boolean.class) {
                field.set(entity, faker.bool().bool());
            } else if (type.isEnum()) {
                Object[] enumValues = type.getEnumConstants();
                if (enumValues.length > 0) {
                    int randomIndex = faker.number().numberBetween(0, enumValues.length);
                    field.set(entity, enumValues[randomIndex]);
                }
            }
            // Add more types as needed
            
        } catch (Exception e) {
            // Log but continue with other fields
            System.err.println("Could not set value for field: " + field.getName() + ", error: " + e.getMessage());
        }
    }
    
    // Get the maximum length for a field from annotations
    private int getMaxFieldLength(Field field) {
        if (field.getName().equals("firstName") || field.getName().equals("first_name")) {
            return 30;
        } else if (field.getName().equals("lastName") || field.getName().equals("last_name")) {
            return 30;
        } else if (field.getName().equals("email")) {
            return 50;
        } else if (field.getName().equals("password")) {
            return 60;
        } else if (field.getName().equals("username")) {
            return 30;
        } else if (field.getName().equals("provider")) {
            return 10;
        }
        
        // Try to get the length from annotations
        try {
            if (field.isAnnotationPresent(jakarta.persistence.Column.class)) {
                jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);
                return column.length();
            }
        } catch (Exception e) {
            // Ignore and use default
        }
        
        // Default safe length
        return 10;
    }
    
    private JpaRepository findRepositoryForEntityClass(Class<?> entityClass) {
        // Try to find repository by naming convention
        String repositoryName = entityClass.getSimpleName() + "Repository";
        String beanName = repositoryName.substring(0, 1).toLowerCase() + repositoryName.substring(1);
        
        try {
            return (JpaRepository) context.getBean(beanName);
        } catch (NoSuchBeanDefinitionException e) {
            // Try all repositories
            Map<String, JpaRepository> repositories = context.getBeansOfType(JpaRepository.class);
            
            for (JpaRepository repository : repositories.values()) {
                Class<?> repoEntityClass = getEntityClassForRepository(repository);
                if (repoEntityClass != null && repoEntityClass.equals(entityClass)) {
                    return repository;
                }
            }
            
            return null;
        }
    }
    
    // Helper methods
    private String getEntityNameFromTableName(String tableName) {
        // Convert from snake_case to CamelCase
        String[] parts = tableName.split("_");
        StringBuilder entityName = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                entityName.append(part.substring(0, 1).toUpperCase());
                if (part.length() > 1) {
                    entityName.append(part.substring(1));
                }
            }
        }
        return entityName.toString();
    }
    
    private Object findRepositoryForEntity(String entityName) {
        // Try to find the repository by naming convention
        try {
            String repositoryName = entityName + "Repository";
            return context.getBean(repositoryName.substring(0, 1).toLowerCase() + repositoryName.substring(1));
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }
    
    private Class<?> getEntityClassForRepository(JpaRepository repository) {
        // Use reflection to get the entity type
        Type[] genericInterfaces = repository.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                Type[] typeArguments = paramType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    return (Class<?>) typeArguments[0];
                }
            }
        }
        return null;
    }
}