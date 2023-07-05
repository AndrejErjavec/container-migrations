library(tidyverse)

migrationsSingle = read.csv('migrations-single.csv')
migrationsMultipleDiff = read.csv('migrations-multiple-diff.csv')
migrationsMultipleStdDev = read.csv('migrations-multiple-stddev.csv')

migrationsSingle %>% mutate(migration = row_number()) -> migrationsSingle
migrationsMultipleDiff %>% mutate(migration = row_number()) -> migrationsMultipleDiff
migrationsMultipleStdDev %>% mutate(migration = row_number()) -> migrationsMultipleStdDev

# view(migrationsSingle)
# view(migrationsMultiple)

# STDDEV ~ MIGRATION
migrationsSingle %>% rbind(migrationsMultipleDiff, migrationsMultipleStdDev) %>% 
  ggplot() +
  geom_line(aes(x=migration, y=stddevCPU, group=algorithm, color=algorithm)) +
  xlim(0, 1000) +
  facet_wrap(~algorithm) +
  theme(legend.position="none")

# STDDEV ~ BLOCK
migrationsSingle %>% rbind(migrationsMultipleDiff, migrationsMultipleStdDev) %>% 
  group_by(algorithm, block) %>% 
  #{summarise(stddev = min(stddevCPU));.} %>% 
  ggplot() +
  geom_line(aes(x=block, y=stddevCPU, color=algorithm)) +
  xlim(0, 120) +
  facet_wrap(~algorithm) +
  theme(legend.position="none")
