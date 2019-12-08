import { val } from '../val';
import { adapt } from './AnimatedBlock';
import AnimatedNode from './AnimatedNode';

export default class AnimatedCallFunc extends AnimatedNode {
  _what;
  _args;
  _params;
  constructor(what, args, params, config = {}) {
    super(
      {
        type: 'callfunc',
        what: what.__nodeID,
        args: args.map(n => n.__nodeID),
        params: params.map(n => n.__nodeID),
        ...config
      },
      [...args]
    );
    this._what = what;
    this._args = args;
    this._params = params;
  }

  beginContext() {
    this._params.forEach((param, index) => {
      param.beginContext(this._args[index]);
    });
  }

  endContext() {
    this._params.forEach((param, index) => {
      param.endContext();
    });
  }

  __onEvaluate() {
    this.beginContext();
    const value = val(this._what);
    this.endContext();
    return value;
  }

  __source() {
    return this._what.__source();
  }
}

export function createAnimatedCallFunc(proc, args, params) {
  return new AnimatedCallFunc(proc, args.map(p => adapt(p)), params);
}
