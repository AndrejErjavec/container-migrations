library(tidyverse)

migrationsSingle = read.csv('migrations-single.csv')
migrationsMultiple = read.csv('migrations-multiple.csv')

migrationsSingle %>% mutate(migration = row_number(), algorithm = "old") -> migrationsSingle
migrationsMultiple %>% mutate(migration = row_number(), algorithm = "new") -> migrationsMultiple

# View(migrationsSingle)
# view(migrationsMultiple)

migrationsMultiple %>% rbind(migrationsSingle) %>% 
  ggplot() +
  geom_line(aes(x=migration, y=stddevCPU, group=algorithm, color=algorithm))