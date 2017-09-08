import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let commands = ['echo', 'broadcast', 'connect', 'disconnect', 'users']
let prevCommand = ''
let useContents = ''
let hostAdd = 'localhost'
let portAdd = 8080

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> <host> <port>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    if(args.host !== null && args.port !== null) {
      hostAdd = args.host 
      portAdd = args.port 
    } 
    console.log(hostAdd + ' : ' + portAdd)
    server = connect({ host: hostAdd, port: portAdd }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let message = buffer.toString()
      if (message.includes('\(all\)'))
        this.log(cli.chalk['blue'](message))
      if (message.includes('\(whisper\)'))
        this.log(cli.chalk['yellow'](message))
      if (message.includes('\(echo\)'))
        this.log(cli.chalk['gray'](message))
      if (message.includes('disconnected'))
        this.log(cli.chalk['red'](message))
      if (message.includes('has connected'))
        this.log(cli.chalk['magenta'](message))
      if(message.includes('currently connected'))
        this.log(cli.chalk['cyan'](message))
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [command, ...rest] = input.split(" ")
    const contents = rest.join(' ')
    const whisperCmd = /\@[^\s]+/;
    
    if (command === 'disconnect' || command === 'users') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'broadcast' || whisperCmd.test(command)) {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }
    callback()
  })
