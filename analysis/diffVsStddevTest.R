library(tidyverse)

migrationsMultipleDiff <- read.csv('testdata/diff-stddev/migrations-diff.csv')
migrationsMultipleStddev <- read.csv('testdata/diff-stddev/migrations-stddev.csv')

# View(migrationsMultipleDiff)
# View(migrationsMultipleStddev)

# STDDEV ~ BLOCK
migrationsMultipleDiff %>% rbind(migrationsMultipleStddev) %>% 
  {testCase <<- unique(.$testCase); .} %>% 
  ggplot() +
  geom_line(aes(x=block, y=stddevCPU, color=algorithm)) +
  scale_y_continuous(trans='log10') +
  xlim(0, 100) +
  facet_wrap(~algorithm) +
  theme(legend.position="none") +
  labs(title="Standardna deviacija CPU glede na število blokov", subtitle = paste("test case", testCase))
