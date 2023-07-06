library(tidyverse)

migrationsSingle = read.csv('migrations-single.csv')
migrationsMultipleDiff = read.csv('migrations-multiple-diff.csv')
migrationsMultipleStdDev = read.csv('migrations-multiple-stddev.csv')

migrationsSingle %>% mutate(migration = row_number()) -> migrationsSingle
migrationsMultipleDiff %>% mutate(migration = row_number()) -> migrationsMultipleDiff
migrationsMultipleStdDev %>% mutate(migration = row_number()) -> migrationsMultipleStdDev

# view(migrationsSingle)
# view(migrationsMultipleDiff)
# view(migrationsMultipleStdDev)

# STDDEV ~ BLOCK
migrationsSingle %>% rbind(migrationsMultipleDiff, migrationsMultipleStdDev) %>% 
  {testCase <<- unique(.$testCase); .} %>% 
  ggplot() +
  geom_line(aes(x=block, y=stddevCPU, color=algorithm)) +
  scale_y_continuous(trans='log10') +
  xlim(0, 100) +
  facet_wrap(~algorithm) +
  theme(legend.position="none") +
  labs(title="Standardna deviacija CPU glede na število blokov", subtitle = paste("test case", testCase))


# STDDEV ~ MIGRATION
migrationsSingle %>% rbind(migrationsMultipleDiff, migrationsMultipleStdDev) %>% 
  ggplot() +
  geom_line(aes(x=migration, y=stddevCPU, group=algorithm, color=algorithm)) +
  scale_y_continuous(trans='log10') +
  xlim(0, 1000) +
  facet_wrap(~algorithm) +
  theme(legend.position="none") +
  labs(title="Standardna deviacija CPU glede na število migracij", subtitle = "Worst case")
