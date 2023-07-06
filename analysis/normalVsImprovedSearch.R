library(tidyverse)

normal <- read.csv('testdata/normal-improved-search/diff-normal.csv')
improved <- read.csv('testdata/normal-improved-search/diff-improved.csv')

# STDDEV ~ BLOCK
normal %>% rbind(improved) %>% 
  {testCase <<- unique(.$testCase); .} %>% 
  ggplot() +
  geom_line(aes(x=block, y=stddevCPU, color=algorithm)) +
  scale_y_continuous(trans='log10') +
  xlim(0, 100) +
  facet_wrap(~algorithm) +
  theme(legend.position="none") +
  labs(title="Standardna deviacija CPU glede na število blokov", subtitle = paste("test case", testCase))