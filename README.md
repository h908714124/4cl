The goal is to pull images from https://sys.4chan.org/image/error/banned/250/rid.php

Result is 302. Read Location header and pull.

	curl -v https://sys.4chan.org/image/error/banned/250/rid.php

	./runme
	lein run
