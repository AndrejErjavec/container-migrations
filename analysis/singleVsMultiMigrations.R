library(tidyverse)

migrationsSingle <- read.csv('testdata/single-multi/migrations-single.csv')
migrationsMultiple <- read.csv('testdata/single-multi/migrations-multi.csv')

# STDDEV ~ BLOCK
migrationsSingle %>% rbind(migrationsMultiple) %>% 
  {testCase <<- unique(.$testCase); .} %>% 
  ggplot() +
  geom_line(aes(x=block, y=stddevCPU, color=algorithm)) +
  scale_y_continuous(trans='log10') +
  xlim(0, 1000) +
  facet_wrap(~algorithm) +
  theme(legend.position="none") +
  labs(title="Standardna deviacija CPU glede na število blokov", subtitle = paste("test case", testCase))