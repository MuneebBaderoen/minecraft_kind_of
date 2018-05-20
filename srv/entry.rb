require 'sinatra'

get '/' do
	response
	open('ngrok.log') do |f|
		f.lines.select { |line| line.include?("URL:") }.each do |line|
    		response = "Server available at: <%= line.match(/tcp:\/\/(.+:[0-9]+) /)[1]"
    	end
    end
    response
end