class TimeInterval:
    def __init__(self, start_hour, start_minute, end_hour, end_minute, value):
        self.start_hour = start_hour
        self.start_minute = start_minute
        self.end_hour = end_hour
        self.end_minute = end_minute
        self.value = value

    def __str__(self):
        return f"TimeInterval{{start_hour={self.start_hour}, start_minute={self.start_minute}, " \
               f"end_hour={self.end_hour}, end_minute={self.end_minute}, value={self.value}}}"

    def get_start_hour(self):
        return self.start_hour

    def set_start_hour(self, start_hour):
        self.start_hour = start_hour

    def get_start_minute(self):
        return self.start_minute

    def set_start_minute(self, start_minute):
        self.start_minute = start_minute

    def get_end_hour(self):
        return self.end_hour

    def set_end_hour(self, end_hour):
        self.end_hour = end_hour

    def get_end_minute(self):
        return self.end_minute

    def set_end_minute(self, end_minute):
        self.end_minute = end_minute

    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value