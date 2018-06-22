from tweepy.streaming import StreamListener
from tweepy import OAuthHandler
from tweepy import Stream

access_token = "131556934-LrYRiXzAL3QcRyFN0fdN53EDWhNGfZFnVX59NCnT"
access_token_secret = "JraMtps5lB98d8XoelAF71KHn8ZQ4nshdoSKiFlTz6OHd"
consumer_key = "P4XZ2GUkeqdhIlQMOredBuW05"
consumer_secret = "r5TPb2UcM8bzxq7t5zflRPMHUrCfwNG4GRuVPXypowrpHhTmue"


class StdOutListener(StreamListener):

    def on_data(self, data):
        print data
        return True

    def on_error(self, status):
        print status


if __name__ == '__main__':

    l = StdOutListener()
    auth = OAuthHandler(consumer_key, consumer_secret)
    auth.set_access_token(access_token, access_token_secret)
    stream = Stream(auth, l)

    stream.filter(track=['ImpeachmentDay', 'NaoVaiTerGolpe', 'ForaDilma'])
